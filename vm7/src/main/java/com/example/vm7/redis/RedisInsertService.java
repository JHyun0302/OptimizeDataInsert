package com.example.vm7.redis;

import com.example.vm7.entity.TbDtfHrasAuto;
import com.example.vm7.repository.TbDtfHrasAutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RedisInsertService {

    private static final String REDIS_KEY = "hras-data";

    private final TbDtfHrasAutoRepository repository;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    public RedisInsertService(ObjectMapper objectMapper, StringRedisTemplate redisTemplate, TbDtfHrasAutoRepository repository, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.repository = repository;
        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Transactional
    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void updateDBAndSaveToRedis(int startId, int endId, int batchSize, int vmIndex) {
        timer.record(() -> {
            String redisKey = REDIS_KEY + ":" + vmIndex;
            List<TbDtfHrasAuto> records = repository.findByRange(startId, endId); // 정수 기반으로 startId와 endId 전달
            log.info("Fetched records: {}", records.size());

            // 데이터를 배치 단위로 나눔
            List<List<TbDtfHrasAuto>> batches = splitIntoBatches(records, batchSize);

            for (List<TbDtfHrasAuto> batch : batches) {
                batch.forEach(record -> {
                    record.setWtlvVal(generateRandomValue());
                    record.setFlowVal(generateRandomValue());
                    record.setVelVal(generateRandomValue());
                });
                repository.saveAll(batch);
                log.info("Updated batch of size: {}", batch.size());

                // Redis에 데이터를 한 번에 저장
                saveBatchToRedis(batch, redisKey);
            }

            log.info("Updated records from {} to {}", startId, endId);
        });
    }

    private List<List<TbDtfHrasAuto>> splitIntoBatches(List<TbDtfHrasAuto> records, int batchSize) {
        List<List<TbDtfHrasAuto>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            int end = Math.min(i + batchSize, records.size());
            batches.add(records.subList(i, end));
        }
        return batches;
    }

    private Long generateRandomValue() {
        return new Random().nextLong(1000); // 예: 0~999
    }

    private void saveBatchToRedis(List<TbDtfHrasAuto> batch, String redisKey) {
        try {
            // JSON 변환
            List<String> jsonRecords = batch.stream()
                    .map(record -> {
                        try {
                            return objectMapper.writeValueAsString(record);
                        } catch (Exception e) {
                            log.error("Failed to serialize record: {}", record, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (jsonRecords.isEmpty()) {
                log.warn("No valid records to save to Redis for key: {}", redisKey);
                return;
            }

            // Redis 파이프라인 사용
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                jsonRecords.forEach(json -> connection.rPush(redisKey.getBytes(), json.getBytes()));
                return null;
            });

            log.info("Saved batch of {} records to Redis key: {}", batch.size(), redisKey);
        } catch (Exception e) {
            log.error("Failed to save batch to Redis key: {}", redisKey, e);
        }
    }
}

