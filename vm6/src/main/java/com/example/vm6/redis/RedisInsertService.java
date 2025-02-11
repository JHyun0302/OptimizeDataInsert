package com.example.vm6.redis;

import com.example.vm6.entity.TbDtfHrasAuto;
import com.example.vm6.entity.TbDtfHrasAutoPk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Service
public class RedisInsertService {

    private static final String REDIS_KEY_PREFIX = "hras-data";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private static final Random RANDOM = new Random();

    @Value("${spring.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Value("${spring.application.vm-index}")
    private int vmIndex;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    public RedisInsertService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
public void saveHrasDataInRedis(int batchIndex) {
    int baseIndex = ((vmIndex * 60) + batchIndex) * batchSize;
    String uniqueKey = "[VM-" + vmIndex + "] " + REDIS_KEY_PREFIX + ":" + System.currentTimeMillis();

    List<String> jsonRecords = generateJsonRecords(baseIndex, batchSize);

    if (jsonRecords.isEmpty()) {
        log.warn("No records to insert into Redis for key: {}", uniqueKey);
        return;
    }

    // 🚀 한 번의 Pipeline에서 10,000개씩 묶어서 전송 (최적화)
    int optimalBatchSize = Math.min(batchSize, 10000);

    timer.record(() -> {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (int i = 0; i < jsonRecords.size(); i += optimalBatchSize) {
                    List<String> batch = jsonRecords.subList(i, Math.min(i + optimalBatchSize, jsonRecords.size()));
                    byte[][] values = batch.stream().map(String::getBytes).toArray(byte[][]::new);
                    connection.listCommands().rPush(uniqueKey.getBytes(), values); // 🚀 한 번에 여러 개의 데이터를 `RPUSH`
                }
                return null;
            });
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            log.error("Failed to insert HRAS data into Redis", e);
        }
    });
}


    /**
     * 주어진 범위의 데이터를 JSON 형태로 변환하여 리스트로 반환
     */
    private List<String> generateJsonRecords(int baseIndex, int batchSize) {
        return IntStream.range(0, batchSize)
                .mapToObj(i -> generateDummyRecordJson(baseIndex + i + 1))
                .filter(Objects::nonNull)  // JSON 변환 실패한 경우 제외
                .toList();
    }

    /**
     * 단일 객체를 JSON 문자열로 변환
     */
    private String generateDummyRecordJson(int csIdNumber) {
        try {
            TbDtfHrasAuto record = generateDummyRecord(csIdNumber);
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize HRAS data: CS_ID={}", csIdNumber, e);
            return null;  // 실패한 경우 제외
        }
    }

    private TbDtfHrasAuto generateDummyRecord(int csIdNumber) {
        String csId = "CS_" + UUID.randomUUID().toString();
        long projectId = 1000000 + RANDOM.nextInt(100000); // PROJECT_ID: 랜덤
        String name = "Project-" + csIdNumber; // NAME
        String riverName = "River-" + (RANDOM.nextInt(5) + 1); // RIVER_NAME: River-1 ~ River-5
        String riverReach = "Reach-" + (RANDOM.nextInt(10) + 1); // RIVER_REACH: Reach-1 ~ Reach-10
        long riverCode = 1000 + RANDOM.nextInt(100); // RIVER_CODE
        long wtlvVal = (RANDOM.nextLong() * 500); // WTLV_VAL: 0~500
        long flowVal = (RANDOM.nextLong() * 200); // FLOW_VAL: 0~200
        long velVal = RANDOM.nextLong() * 5; // VEL_VAL: 0~5
        LocalDateTime pdctDt = LocalDateTime.now().minusDays(RANDOM.nextInt(30)); // PDCT_DT: 최근 30일 내 날짜

        TbDtfHrasAutoPk pk = new TbDtfHrasAutoPk(csId, pdctDt);

        return TbDtfHrasAuto.builder()
                .pk(pk)
                .projectId(projectId)
                .name(name)
                .riverName(riverName)
                .riverReach(riverReach)
                .riverCode(riverCode)
                .wtlvVal(wtlvVal)
                .flowVal(flowVal)
                .velVal(velVal)
                .build();
    }
}

