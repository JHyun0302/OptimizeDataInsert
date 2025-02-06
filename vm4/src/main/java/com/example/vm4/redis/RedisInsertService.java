package com.example.vm4.redis;

import com.example.vm4.entity.TbDtfHrasAuto;
import com.example.vm4.entity.TbDtfHrasAutoPk;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisInsertService {

    private static final String REDIS_KEY_PREFIX = "hras-data";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private static final Random RANDOM = new Random();

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Value("${spring.application.vm-index}")
    private int vmIndex;

    public void saveHrasDataInRedis() {
        String uniqueKey = "[VM-"+ vmIndex + "] " + REDIS_KEY_PREFIX + ":" + System.currentTimeMillis();
        TbDtfHrasAuto record = generateDummyRecord(4);
        try {
            for (int i = 0; i < batchSize; i++) {
                String jsonData = objectMapper.writeValueAsString(record);  // 엔티티를 JSON으로 직렬화
                redisTemplate.opsForList().rightPush(uniqueKey, jsonData); // Redis 리스트에 추가
            }
        } catch (Exception e) {
            log.error("Failed to serialize HRAS data: ", e);
        }
        log.info("Inserted {} records in Redis for key: {}", batchSize, uniqueKey);
    }

    private TbDtfHrasAuto generateDummyRecord(int index) {
        String csId = "CS_" + (1000000 + index); // CS_ID 형식: 1000000부터 시작
        long projectId = 1000000 + RANDOM.nextInt(100000); // PROJECT_ID: 랜덤
        String name = "Project-" + index; // NAME
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

