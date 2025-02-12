package com.example.vm1.Thread;

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

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        return Flux.merge(
                        fetchDataFromRedis(redisTemplate),
                        fetchDataFromRedis(secondRedisTemplate)
                )
                .buffer(BATCH_INSERT_SIZE) // 데이터를 묶음
                .flatMap(this::batchInsertData) // List<TbDtfHrasAuto>를 전달하도록 수정
                .then()
                .doOnSuccess(ignored -> log.info("All batches processed successfully"));
    }

    private Flux<TbDtfHrasAuto> fetchDataFromRedis(ReactiveRedisTemplate<String, String> redisTemplate) {
        return scanKeys(redisTemplate, REDIS_KEY_PATTERN)
                .buffer(BATCH_KEY_SIZE) // BATCH_KEY_SIZE 단위로 키를 가져옴
                .flatMap(batchKeys -> fetchBatchFromRedis(redisTemplate, batchKeys)
                        .subscribeOn(Schedulers.boundedElastic()) // 병렬 I/O 스레드 사용
                )
                .flatMapIterable(list -> list);
    }

    private Mono<List<TbDtfHrasAuto>> fetchBatchFromRedis(ReactiveRedisTemplate<String, String> redisTemplate, List<String> keys) {
        return Flux.fromIterable(keys)
                .flatMap(key -> redisTemplate.opsForList().range(key, 0, -1)
                        .flatMap(json -> Mono.justOrEmpty(deserializeJson(json)))
                        .collectList()
                        .doOnNext(list -> redisTemplate.delete(key).subscribe()) // 데이터 삭제
                )
                .flatMapIterable(list -> list)
                .collectList();
    }

    private Flux<String> scanKeys(ReactiveRedisTemplate<String, String> redisTemplate, String pattern) {
        return redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(300) // 한 번에 300개씩 스캔
                .build()
        );
    }

    private Mono<Void> batchInsertData(List<TbDtfHrasAuto> dataList) {
        if (dataList.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(dataList)
                .buffer(BATCH_INSERT_SIZE)
                .flatMap(repository::saveAll, 8) // 병렬 처리
                .as(transactionalOperator::transactional)
                .then();
    }

    private TbDtfHrasAuto deserializeJson(String jsonData) {
        try {
            return objectMapper.readValue(jsonData, TbDtfHrasAuto.class);
        } catch (Exception e) {
            log.error("⚠️ Failed to deserialize HRAS data: ", e);
            return null;
        }
    }
}
