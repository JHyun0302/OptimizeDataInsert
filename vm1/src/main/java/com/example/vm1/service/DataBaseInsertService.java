package com.example.vm1.service;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.repository.TbDtfHrasAutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DataBaseInsertService {

    private static final int BATCH_KEY_SIZE = 50; // 한 번에 가져올 Redis 키 개수
    private static final int BATCH_INSERT_SIZE = 10_000; // 한 번에 DB에 저장할 최대 데이터 개수

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final ReactiveRedisTemplate<String, String> secondRedisTemplate;

    private final TransactionalOperator transactionalOperator;

    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PATTERN = "*VM-*hras-data:*";

    private final TbDtfHrasAutoRepository repository;

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer timer;

    public DataBaseInsertService(@Qualifier("primaryRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
                                 @Qualifier("secondRedisTemplate") ReactiveRedisTemplate<String, String> secondRedisTemplate,
                                 TransactionalOperator transactionalOperator,
                                 ObjectMapper objectMapper,
                                 TbDtfHrasAutoRepository repository,
                                 MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.secondRedisTemplate = secondRedisTemplate;
        this.transactionalOperator = transactionalOperator;
        this.objectMapper = objectMapper;
        this.repository = repository;

        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public Mono<Void> processDataInBatches() {
        AtomicInteger totalCount = new AtomicInteger(0); // 전체 데이터 개수 카운트

        return Flux.concat(
                        fetchDataAndInsert(redisTemplate, totalCount),
                        fetchDataAndInsert(secondRedisTemplate, totalCount)
                )
                .then(Mono.fromRunnable(() -> log.info("Redis에서 가져온 총 데이터 개수: {}", totalCount.get()))) // 전체 개수 로깅
                .doOnSuccess(ignored -> log.info("All batches processed successfully"))
                .then();
    }

    private Flux<Void> fetchDataAndInsert(ReactiveRedisTemplate<String, String> redisTemplate, AtomicInteger totalCount) {
        return scanKeys(redisTemplate, REDIS_KEY_PATTERN)
                .buffer(100) // 한 번에 100개 키 가져오기
                .concatMap(batchKeys -> fetchBatchFromRedis(redisTemplate, batchKeys, totalCount)
                        .flatMap(this::batchInsertData) // 가져온 데이터를 즉시 DB에 삽입
                );
    }

    private Mono<List<TbDtfHrasAuto>> fetchBatchFromRedis(ReactiveRedisTemplate<String, String> redisTemplate, List<String> keys, AtomicInteger totalCount) {
        return Flux.fromIterable(keys)
                .flatMap(key -> redisTemplate.opsForList().range(key, 0, -1)
                        .flatMap(json -> {
                            TbDtfHrasAuto data = deserializeJson(json);
                            if (data == null || data.getCsId() == null || data.getPdctDt() == null) {
                                return Mono.empty(); // NULL 데이터 제외
                            }
                            return Mono.just(data);
                        })
                        .collectList()
                        .flatMap(list -> redisTemplate.delete(key)
                                .thenReturn(list) // 삭제 후 데이터 반환
                        )
                )
                .flatMapIterable(list -> list)
                .doOnNext(data -> totalCount.incrementAndGet()) // 전체 개수 증가
                .collectList()
                .doOnSuccess(list -> log.info("Redis에서 가져온 데이터 개수: {}", list.size())); // 가져온 개수만 로깅
    }

    private Flux<String> scanKeys(ReactiveRedisTemplate<String, String> redisTemplate, String pattern) {
        return redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(100) // 🔥 한 번에 100개씩 키 스캔
                .build()
        );
    }

    private Mono<Void> batchInsertData(List<TbDtfHrasAuto> dataList) {
        return transactionalOperator.execute(transaction ->
                Flux.fromIterable(dataList)
                        .buffer(BATCH_INSERT_SIZE)
                        .concatMap(batch -> {
                            log.info("🔥 DB에 {}건 삽입 시도 중...", batch.size());
                            return Flux.fromIterable(batch)
                                    .concatMap(data -> repository.insertAuto(
                                            data.getCsId(),
                                            data.getPdctDt(),
                                            data.getProjectId(),
                                            data.getName(),
                                            data.getRiverName(),
                                            data.getRiverReach(),
                                            data.getRiverCode(),
                                            data.getWtlvVal(),
                                            data.getFlowVal(),
                                            data.getVelVal()
                                    ).doOnSuccess(v -> log.info("✅ DB INSERT 성공: {}", data.getCsId())))
                                    .onErrorContinue((throwable, obj) -> log.error("DB INSERT 오류 발생: {}, 데이터: {}", throwable.getMessage(), obj));
                        })
                        .collectList() // Flux → Mono 변환 (commit 보장)
                        .thenReturn(transaction) // 트랜잭션 유지
        ).then(); // 최종적으로 Mono<Void> 반환
    }

    private TbDtfHrasAuto deserializeJson(String jsonData) {
        try {
            TbDtfHrasAuto auto = objectMapper.readValue(jsonData, TbDtfHrasAuto.class);
            if (auto.getPk() != null) {
                auto.setCsId(auto.getPk().getCsId());  // 🔥 pk 값을 직접 매핑
                auto.setPdctDt(auto.getPk().getPdctDt());
            }
            if (auto.getCsId() == null || auto.getPdctDt() == null) {
                log.error("JSON에서 PK 필드가 NULL입니다! JSON: {}", jsonData);
                return null;
            }
            return auto;
        } catch (Exception e) {
            log.error("⚠️ JSON Deserialization 오류 발생: ", e);
            return null;
        }
    }

}
