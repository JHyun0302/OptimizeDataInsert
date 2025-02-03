package com.example.vm4.redis;


import com.example.vm4.entity.TbDtfHrasAuto;
import com.example.vm4.repository.TbDtfHrasAutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class SaveInDataBase {

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "hras-data";

    private final TbDtfHrasAutoRepository repository;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    public SaveInDataBase(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, TbDtfHrasAutoRepository repository, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void transferHrasDataToDB() {
        timer.record(() -> {
            try {
                // Redis에서 모든 데이터 가져오기
                Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + ":*");

                if (keys != null && !keys.isEmpty()) {
                    for (String key : keys) {
                        List<String> jsonDataList = redisTemplate.opsForList().range(key, 0, -1); // Redis 리스트에서 데이터 가져오기

                        if (jsonDataList != null && !jsonDataList.isEmpty()) {
                            List<TbDtfHrasAuto> hrasDataList = new ArrayList<>();

                            // JSON 데이터를 역직렬화하여 엔티티 리스트로 변환
                            jsonToEntity(jsonDataList, hrasDataList);
                            saveToDatabase(hrasDataList); // DB에 저장
                            redisTemplate.delete(key); // Redis에서 해당 키 삭제
                        }
                    }
                    successCounter.increment();
                }
            } catch (Exception e) {
                failureCounter.increment();
                log.error("Failed to transfer HRAS data to DB: ", e.getMessage());
            }
        });
    }

    private void jsonToEntity(List<String> jsonDataList, List<TbDtfHrasAuto> hrasDataList) {
        for (String jsonData : jsonDataList) {
            try {
                TbDtfHrasAuto hrasData = objectMapper.readValue(jsonData, TbDtfHrasAuto.class);
                hrasDataList.add(hrasData);
            } catch (Exception e) {
                log.error("Failed to deserialize HRAS data: ", e);
            }
        }
    }


    private void saveToDatabase(List<TbDtfHrasAuto> hrasDataList) {
        if (hrasDataList.isEmpty()) {
            log.info("No HRAS data to save.");
            return;
        }

        log.info("Saving {} HRAS records to database", hrasDataList.size());
        for (TbDtfHrasAuto tbDtfHrasAuto : hrasDataList) {
            repository.save(tbDtfHrasAuto);
        }
        repository.flush();
        hrasDataList.clear();
    }
}
