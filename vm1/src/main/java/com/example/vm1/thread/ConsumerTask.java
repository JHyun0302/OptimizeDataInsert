package com.example.vm1.thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ConsumerTask implements Runnable {

    private final BlockingQueue<UpdateTask> queue;

    private final ObjectMapper objectMapper;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void run() {
        try {
            while (true) {
                // 1) 큐에서 UpdateTask를 하나 꺼냄
                UpdateTask task = queue.take();
                // 2) 이 정보를 가지고 Redis에 반영
                updateBatchInRedis(task.getRedisKey(), task.getRecords());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Consumer] Interrupted!");
        }
    }

    private void updateBatchInRedis(String redisKey, List<TbDtfHrasAuto> records) {
        try {
            List<String> updatedJsonRecords = records.stream()
                    .map(record -> {
                        try {
                            return objectMapper.writeValueAsString(record);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());

            redisTemplate.delete(redisKey);
            if (!updatedJsonRecords.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(redisKey, updatedJsonRecords);
            }
            log.info("Updated {} records in Redis for key: {}", updatedJsonRecords.size(), redisKey);
        } catch (Exception e) {
            log.error("Failed to update batch in Redis for key: {}", redisKey, e);
        }
    }
}

