package com.example.vm9.redis;

import com.example.vm9.entity.TbDtfHrasAuto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisStreamService {

    private static final String STREAM_KEY = "dummyDataStream";

    private final StringRedisTemplate redisTemplate;

    public void writeToStream(TbDtfHrasAuto data) {
        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();

        Map<String, String> record = new HashMap<>();
        record.put("csId", data.getPk().getCsId());
        record.put("pdctDt", data.getPk().getPdctDt().toString());
        record.put("projectId", data.getProjectId().toString());
        record.put("name", data.getName());
        record.put("riverName", data.getRiverName());
        record.put("riverReach", data.getRiverReach());
        record.put("riverCode", data.getRiverCode().toString());
        record.put("wtlvVal", data.getWtlvVal().toString());
        record.put("flowVal", data.getFlowVal().toString());
        record.put("velVal", data.getVelVal().toString());

        streamOps.add(STREAM_KEY, record); // Redis Streams에 데이터 추가
    }
}

