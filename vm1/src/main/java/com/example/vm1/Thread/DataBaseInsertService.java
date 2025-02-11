package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private static final int BATCH_KEY_SIZE = 30; // 한 번에 가져올 키 개수 (30개)

    private static final int BATCH_INSERT_SIZE = 10000; // 한 번에 DB에 Insert할 개수 (10000개)

    public DataBaseInsertService(RedisTemplate<String, String> redisTemplate,
                                 RedisTemplate<String, String> secondRedisTemplate,
                                 ObjectMapper objectMapper,
                                 @Qualifier("multiThreadBatchInsertRunner") BatchInsertRunner batchInsertRunner) {
        this.redisTemplate = redisTemplate;
        this.secondRedisTemplate = secondRedisTemplate;
        this.objectMapper = objectMapper;
        this.batchInsertRunner = batchInsertRunner;
        this.executorService = Executors.newFixedThreadPool(6); // Redis 조회는 CPU보다는 네트워크 I/O가 중요 → 6개 스레드 사용
    }

    public void processDataInBatches() {
        log.info("Starting batch data retrieval and insert...");

        while (true) {
            List<TbDtfHrasAuto> dataList = new ArrayList<>();

            // Redis에서 일정 개수의 키를 가져와 처리
            dataList.addAll(fetchDataFromRedis(redisTemplate));
            dataList.addAll(fetchDataFromRedis(secondRedisTemplate));

            if (dataList.isEmpty()) {
                log.info("No more data to process. Stopping...");
                break;
            }

            // 가져온 데이터를 바로 DB에 Insert
            batchInsertRunner.runBatchInsert(dataList);
        }
    }

    private List<TbDtfHrasAuto> fetchDataFromRedis(RedisTemplate<String, String> redisTemplate) {
        Set<String> keys = redisTemplate.keys(REDIS_KEY_PATTERN);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<TbDtfHrasAuto> allData = new ArrayList<>();
        List<String> keyList = new ArrayList<>(keys);
        int totalKeys = keyList.size();

        // ✅ 키를 일정 개수(BATCH_KEY_SIZE)씩 가져와 처리
        for (int i = 0; i < totalKeys; i += BATCH_KEY_SIZE) {
            int endIdx = Math.min(i + BATCH_KEY_SIZE, totalKeys);
            List<String> subKeys = keyList.subList(i, endIdx);

            try {
                List<TbDtfHrasAuto> batchData = subKeys.parallelStream()
                        .map(key -> redisTemplate.opsForList().range(key, 0, -1))
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .map(this::deserializeJson)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                allData.addAll(batchData);

                // 한 번에 5000개 이상 가져오면 바로 DB Insert 실행
                if (allData.size() >= BATCH_INSERT_SIZE) {
                    batchInsertRunner.runBatchInsert(new ArrayList<>(allData));
                    allData.clear();
                }

            } catch (Exception e) {
                log.error("Error while retrieving data from Redis", e);
            }
        }

        return allData;
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
