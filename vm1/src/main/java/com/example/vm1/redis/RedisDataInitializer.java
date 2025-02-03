package com.example.vm1.redis;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.repository.TbDtfHrasAutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisDataInitializer {

    private static final String REDIS_KEY_PREFIX = "hras-data:";

    private final TbDtfHrasAutoRepository repository;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    @Value("${spring.application.vm-index}")  // VM의 고유 인덱스 (1~16)
    private int vmIndex;

    @Value("${spring.application.total-vms}") // 총 VM 개수
    private int totalVms;


    public RedisDataInitializer(TbDtfHrasAutoRepository repository,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        String redisKey = REDIS_KEY_PREFIX + vmIndex;
        log.info("Initializing Redis with data from DB for key: {}", redisKey);

        redisTemplate.delete(redisKey);

        int dataPerVm = 12_000_000 / totalVms;
        int startId = (vmIndex * dataPerVm) + 1;
        int endId = (vmIndex + 1) * dataPerVm;
        if (vmIndex == totalVms - 1) { // 마지막 VM이면 나머지 데이터도 포함
            endId += 12_000_000 % totalVms;
        }
        log.info("VM {} will load records with ID range from {} to {}", vmIndex, startId, endId);

        List<TbDtfHrasAuto> records = repository.findByCsIdRange(startId, endId);

        if (records == null || records.isEmpty()) {
            log.warn("No data found in DB for range {} to {}.", startId, endId);
            return;
        }

        // 각 객체를 JSON 문자열로 변환
        List<String> jsonRecords = records.stream()
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

        // Redis의 리스트에 데이터를 저장
        if (!jsonRecords.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(redisKey, jsonRecords);
            log.info("Loaded {} records into Redis for key: {}", jsonRecords.size(), redisKey);
        } else {
            log.warn("No valid records to load into Redis after serialization.");
        }
    }
}
