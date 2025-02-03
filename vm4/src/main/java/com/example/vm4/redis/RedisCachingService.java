package com.example.vm4.redis;

import com.example.vm4.entity.TbDtfHrasAuto;
import com.example.vm4.repository.TbDtfHrasAutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
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
public class RedisCachingService {

    private static final String REDIS_KEY = "hras-data";

    private final TbDtfHrasAutoRepository repository;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    public RedisCachingService(ObjectMapper objectMapper, StringRedisTemplate redisTemplate, TbDtfHrasAutoRepository repository, MeterRegistry meterRegistry) {
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
    public void updateWithRedisCaching(int startId, int endId, int vmIndex) {
        timer.record(() -> {
            String redisKey = REDIS_KEY + ":" + vmIndex;

            // Redis에서 해당 키의 전체 데이터를 읽음 (JSON 문자열 목록)
            List<String> jsonRecords = redisTemplate.opsForList().range(redisKey, 0, -1);
            if (jsonRecords == null || jsonRecords.isEmpty()) {
                log.warn("No records found in Redis for key: {}", redisKey);
                return;
            }

            // JSON 문자열을 TbDtfHrasAuto 객체로 변환
            List<TbDtfHrasAuto> records = new ArrayList<>();
            for (String json : jsonRecords) {
                try {
                    TbDtfHrasAuto record = objectMapper.readValue(json, TbDtfHrasAuto.class);
                    records.add(record);
                } catch (Exception e) {
                    log.error("Failed to deserialize JSON: {}", json, e);
                }
            }

            Long randomWtlv = (long) (Math.random() * 100 + 1);
            Long randomFlow = (long) (Math.random() * 100 + 1);
            Long randomVel = (long) (Math.random() * 100 + 1);

            int updatedRows = repository.bulkUpdateByRange(randomWtlv, randomFlow, randomVel, startId, endId);
            log.info("Bulk updated {} rows for range {} to {}", updatedRows, startId, endId);

            // Redis에 해당 배치의 업데이트된 데이터를 반영
            updateBatchInRedis(redisKey, records);
        });
    }

    private Long generateRandomValue() {
        return new Random().nextLong(1000); // 예: 0~999
    }

    private void updateBatchInRedis(String redisKey, List<TbDtfHrasAuto> records) {
        try {
            // 배치의 각 객체를 JSON 문자열로 변환
            List<String> updatedJsonRecords = records.stream()
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

            redisTemplate.delete(redisKey);
            redisTemplate.opsForList().rightPushAll(redisKey, updatedJsonRecords);
            log.info("Updated {} records in Redis for key: {}", updatedJsonRecords.size(), redisKey);
        } catch (Exception e) {
            log.error("Failed to update batch in Redis for key: {}", redisKey, e);
        }
    }
}

