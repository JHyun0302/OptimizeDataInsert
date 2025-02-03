package com.example.vm9.redis;


import com.example.vm9.entity.TbDtfHrasAuto;
import com.example.vm9.repository.TbDtfHrasAutoRepository;
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
import java.util.Set;

@Service
@Slf4j
public class SaveInDataBase {

    private static final String REDIS_KEY = "hras-data";

    private final TbDtfHrasAutoRepository repository;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    public SaveInDataBase(ObjectMapper objectMapper, StringRedisTemplate redisTemplate, TbDtfHrasAutoRepository repository, MeterRegistry meterRegistry) {
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
    public void readFromRedisAndInsertToDB() {
        timer.record(() -> {
            Set<String> keys = redisTemplate.keys(REDIS_KEY + ":*");

            for (String key : keys) {
                List<String> jsonDataList = redisTemplate.opsForList().range(key, 0, -1); // Redis 리스트에서 데이터 가져오기

                List<TbDtfHrasAuto> hrasDataList = new ArrayList<>();
                for (String jsonData : jsonDataList) {
                    try {
                        TbDtfHrasAuto hrasData = objectMapper.readValue(jsonData, TbDtfHrasAuto.class);
                        hrasDataList.add(hrasData);
                    } catch (Exception e) {
                        log.error("Failed to deserialize HRAS data from Redis key {}: {}", key, e.getMessage());
                    }
                }

                if (!hrasDataList.isEmpty()) {
                    repository.saveAll(hrasDataList); // DB에 배치 저장
                    repository.flush();
                    hrasDataList.clear();
                    log.info("Saved {} HRAS records to DB from Redis key: {}", hrasDataList.size(), key);
                }

                redisTemplate.delete(key); // 처리 완료 후 Redis 키 삭제
                log.info("Deleted Redis key: {}", key);
            }
        });
    }
}
