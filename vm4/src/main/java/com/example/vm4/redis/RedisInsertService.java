package com.example.vm4.redis;

import com.example.vm4.entity.TbDtfHrasAuto;
import com.example.vm4.entity.TbDtfHrasAutoPk;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

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
        // VM별 CS_ID 시작점을 설정 (5만 개씩 증가)
        int baseIndex = ((vmIndex * 6) + batchIndex) * batchSize;

        // Redis 키는 VM별로 다르게 생성
        String uniqueKey = "[VM-" + vmIndex + "] " + REDIS_KEY_PREFIX + ":" + System.currentTimeMillis();

        timer.record(() -> {
            try {
                for (int i = 0; i < batchSize; i++) {
                    int csIdNumber = baseIndex + i + 1; // CS_ID 1부터 증가
                    TbDtfHrasAuto record = generateDummyRecord(csIdNumber);
                    String jsonData = objectMapper.writeValueAsString(record);
                    redisTemplate.opsForList().rightPush(uniqueKey, jsonData);
                }
                successCounter.increment();
            } catch (Exception e) {
                failureCounter.increment();
                log.error("Failed to serialize HRAS data: ", e);
            }
        });
        //log.info("Inserted {} records in Redis for key: {}", batchSize, uniqueKey);
    }

    private TbDtfHrasAuto generateDummyRecord(int csIdNumber) {
        String csId = "CS_" + csIdNumber; // CS_ID 형식: 1부터 시작하여 1씩 증가
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

