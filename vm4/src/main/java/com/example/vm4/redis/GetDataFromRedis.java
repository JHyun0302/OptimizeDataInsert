package com.example.vm4.redis;

import com.example.vm4.entity.TbDtfHrasAuto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GetDataFromRedis {

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "hras-data";


    public GetDataFromRedis(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<TbDtfHrasAuto> getData(int vmIndex) {
        String currentVMKeyPrefix = "*VM-" + vmIndex + "*hras-data:*";

        // Redis에서 현재 VM에 해당하는 키만 가져오기
        Set<String> keys = redisTemplate.keys(currentVMKeyPrefix);
        log.info("keys.size(): {}", keys.size());
//        log.info("currentVMKeyPrefix, keys.size() = {}, {}", currentVMKeyPrefix, keys.size());

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return keys.stream()
                    .map(key -> redisTemplate.opsForList().range(key, 0, -1))  // 각 키에 대해 데이터 가져오기
                    .filter(Objects::nonNull)  // null 값 제거
                    .flatMap(List::stream)  // List<List<String>> → List<String> 변환
                    .map(this::deserializeJson)  // JSON을 객체로 변환
                    .filter(Objects::nonNull)  // 변환 실패한 값 제외
                    .collect(Collectors.toList());  // 최종 리스트로 수집
        } catch (Exception e) {
            log.error("Failed to transfer HRAS data to DB: ", e);
            return Collections.emptyList();
        }
    }

    public List<TbDtfHrasAuto> getData(int groupIndex, int keySize) {
        String keyPattern = "*VM-*hras-data:*";
        Set<String> keys = redisTemplate.keys(keyPattern);

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        // 키 정렬
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);

        // 그룹별로 30개씩 가져오기
        int startIdx = groupIndex * keySize;
        int endIdx = Math.min(startIdx + keySize, sortedKeys.size());

        List<String> groupKeys = sortedKeys.subList(startIdx, endIdx);
//        log.info("Group-{}, Selected Keys: {}", groupIndex, groupKeys.size());

        return groupKeys.stream()
                .map(key -> redisTemplate.opsForList().range(key, 0, -1))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(this::deserializeJson)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
