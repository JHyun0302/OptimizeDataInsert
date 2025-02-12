package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

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

    private final RedisTemplate<String, String> redisTemplate;

    private final RedisTemplate<String, String> secondRedisTemplate;

    private final ObjectMapper objectMapper;

    private final BatchInsertRunner batchInsertRunner;

    private final ExecutorService executorService;

    private static final String REDIS_KEY_PATTERN = "*VM-*hras-data:*";

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer timer;

    public DataBaseInsertService(RedisTemplate<String, String> redisTemplate,
                                 @Qualifier("secondRedisTemplate") RedisTemplate<String, String> secondRedisTemplate,
                                 ObjectMapper objectMapper,
                                 @Qualifier("multiThreadBatchInsertRunner") BatchInsertRunner batchInsertRunner,
                                 MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.secondRedisTemplate = secondRedisTemplate;
        this.objectMapper = objectMapper;
        this.batchInsertRunner = batchInsertRunner;
        this.executorService = Executors.newFixedThreadPool(6); // Redis 조회는 CPU보다는 네트워크 I/O가 중요 → 6개 스레드 사용

        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void processDataInBatches() {
        AtomicInteger totalInserted = new AtomicInteger();

        boolean isPrimaryRedisEmpty = false;   // Primary Redis가 비었는지 확인하는 플래그
        boolean isSecondaryRedisEmpty = false; // Secondary Redis가 비었는지 확인하는 플래그

        while (true) {
            int dataCount1 = isPrimaryRedisEmpty ? 0 : fetchDataFromRedis(redisTemplate);
            int dataCount2 = isSecondaryRedisEmpty ? 0 : fetchDataFromRedis(secondRedisTemplate);

            if (dataCount1 == 0) {
                isPrimaryRedisEmpty = true;
            }
            if (dataCount2 == 0) {
                isSecondaryRedisEmpty = true;
            }

            // 두 개의 Redis에서 모두 데이터를 가져왔는데 비어 있다면 종료
            if (isPrimaryRedisEmpty && isSecondaryRedisEmpty) {
                log.info("No more data to process. Stopping...");
                break;
            }

            totalInserted.addAndGet(dataCount1 + dataCount2);
        }

        log.info("✅ All batches processed successfully. Total Inserted: {}", totalInserted.get());
    }

    private int fetchDataFromRedis(RedisTemplate<String, String> redisTemplate) {
        Set<String> keys = scanKeys(redisTemplate, REDIS_KEY_PATTERN);

        if (keys.isEmpty()) {
            return 0;
        }

        List<String> keyList = new ArrayList<>(keys);

        int totalFetched = 0;
        List<TbDtfHrasAuto> totalBatchData = new ArrayList<>();

        int BATCH_KEY_SIZE = 100; // 🚀 한 번에 100개씩 요청

        for (int i = 0; i < keyList.size(); i += BATCH_KEY_SIZE) {
            int endIdx = Math.min(i + BATCH_KEY_SIZE, keyList.size());
            List<String> subKeys = keyList.subList(i, endIdx);

            try {
                List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String key : subKeys) {
                        connection.listCommands().lRange(key.getBytes(), 0, -1);
                    }
                    return null;
                });

                log.info("✅ lRange executed for {} keys, results size: {}", subKeys.size(), results.size());

                for (int j = 0; j < results.size(); j++) {
                    Object result = results.get(j);

                    if (result instanceof List) {
                        List<String> jsonList = (List<String>) result;
                        List<TbDtfHrasAuto> batchData = jsonList.stream()
                                .map(this::deserializeJson)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        totalFetched += batchData.size();
                        totalBatchData.addAll(batchData);
                    }
                }
            } catch (Exception e) {
                log.error("⚠️ Error fetching data from Redis in batch {}-{} : {}", i, endIdx, e.getMessage());
            }
        }

        if (!totalBatchData.isEmpty()) {
            batchInsertRunner.runBatchInsert(totalBatchData);
            redisTemplate.delete(keyList); // 데이터 Insert 후 삭제
        }

        return totalFetched;
    }

    private Set<String> scanKeys(RedisTemplate<String, String> redisTemplate, String pattern) {
        Set<String> keys = new HashSet<>();

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)  // 패턴 매칭
                .count(300)      // 한 번에 100개씩 가져오기
                .build();

        Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                redisConnection -> redisConnection.scan(options)
        );

        if (cursor != null) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
            cursor.close();
        }

        return keys;
    }

    private TbDtfHrasAuto deserializeJson(String jsonData) {
        try {
            return objectMapper.readValue(jsonData, TbDtfHrasAuto.class);
        } catch (Exception e) {
            log.error("Failed to deserialize HRAS data: ", e);
            return null;
        }
    }
}
