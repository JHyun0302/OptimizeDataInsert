package com.example.vm7.redis;

import com.example.vm7.entity.TbDtfHrasAuto;
import com.example.vm7.repository.TbDtfHrasAutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RedisDataUpdateService {

    private static final String REDIS_KEY = "hras-data";
    private final TbDtfHrasAutoRepository repository;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    public RedisDataUpdateService(ObjectMapper objectMapper, StringRedisTemplate redisTemplate, TbDtfHrasAutoRepository repository, MeterRegistry meterRegistry) {
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
    public void readFromRedisAndInsertToDB(int batchSize) {
        timer.record(() -> {
            List<String> jsonDataList = redisTemplate.opsForList().range(REDIS_KEY, 0, batchSize - 1);
            if (jsonDataList == null || jsonDataList.isEmpty()) {
                log.info("No data found in Redis.");
                return;
            }

            List<TbDtfHrasAuto> records = new ArrayList<>();
            for (String jsonData : jsonDataList) {
                try {
                    TbDtfHrasAuto record = objectMapper.readValue(jsonData, TbDtfHrasAuto.class); // 역직렬화
                    records.add(record);
                } catch (Exception e) {
                    log.error("Failed to deserialize Redis data: {}", e.getMessage());
                }
            }

            repository.saveAll(records);
            repository.flush();
            records.clear();

            // Redis에서 처리된 데이터 삭제
            redisTemplate.opsForList().trim("hras-data", batchSize, -1);
            log.info("Updated {} records in DB and removed from Redis.", records.size());
        });
    }

    @Transactional
    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void updateDBAndSaveToRedis(int startId, int endId, int batchSize) {
        timer.record(() -> {
            log.info("startId, endId = {}, {}", startId, endId);
            // 정수 기반으로 startId와 endId 전달
            List<TbDtfHrasAuto> records = repository.findByRange(startId, endId);
            log.info("Fetched records: {}", records.size()); // Fetched records: 1,200,000

            // 데이터를 배치 단위로 나눔
            List<List<TbDtfHrasAuto>> batches = splitIntoBatches(records, batchSize);

            for (List<TbDtfHrasAuto> batch : batches) {
                batch.forEach(record -> {
                    repository.updateValues(
                            generateRandomValue(), // wtlvVal
                            generateRandomValue(), // flowVal
                            generateRandomValue(), // velVal
                            record.getPk().getCsId(),
                            record.getPk().getPdctDt()
                    );
                    saveToRedis(record); // update한 데이터를 Redis에 저장
                });
            }
            log.info("Updated records from {} to {}", startId, endId);
        });
    }

    private List<List<TbDtfHrasAuto>> splitIntoBatches(List<TbDtfHrasAuto> records, int batchSize) {
        List<List<TbDtfHrasAuto>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            int end = Math.min(i + batchSize, records.size());
            batches.add(records.subList(i, end));
        }
        return batches;
    }

    private Long generateRandomValue() {
        return new Random().nextLong(1000); // 예: 0~999
    }

    private void saveToRedis(TbDtfHrasAuto record) {
        try {
            // 객체를 JSON 형식으로 직렬화
            String jsonRecord = objectMapper.writeValueAsString(record);

            // Redis에 JSON 데이터 저장
            redisTemplate.opsForList().rightPush(REDIS_KEY, jsonRecord);

            log.info("Saved record to Redis: {}", jsonRecord);
        } catch (Exception e) {
            log.error("Failed to save record to Redis: {}", record, e);
        }
    }
}