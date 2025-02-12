package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataBaseInsertServiceServiceV2 implements DataBaseInsertService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, String> secondRedisTemplate;

    private final ObjectMapper objectMapper;

    private final BatchInsertRunner batchInsertRunner;

    private final HikariDataSource hikariDataSource;

    private final ExecutorService executorService;

    private static final String REDIS_KEY_PATTERN = "*VM-*hras-data:*";

    private static final int MAX_BATCH_PER_CYCLE = 3_000_000; // 한 번에 300만 개만 처리
    private static final int BATCH_KEY_SIZE = 50; // 한 번에 Redis에서 가져오는 키 개수
    private static final int QUEUE_CAPACITY = 1_000_000; // 100만 개 배치 저장
    private static final int THREAD_COUNT = 8; // 병렬로 DB Insert할 스레드 수
    private static final int REDIS_FETCH_LIMIT = 20_000; // 한 키에서 가져올 최대 데이터 개수


    private final BlockingQueue<List<TbDtfHrasAuto>> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer timer;

    public DataBaseInsertServiceServiceV2(RedisTemplate<String, String> redisTemplate,
                                          @Qualifier("secondRedisTemplate") RedisTemplate<String, String> secondRedisTemplate,
                                          ObjectMapper objectMapper,
                                          @Qualifier("multiThreadBatchInsertRunner") BatchInsertRunner batchInsertRunner, HikariDataSource hikariDataSource,
                                          MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.secondRedisTemplate = secondRedisTemplate;
        this.objectMapper = objectMapper;
        this.batchInsertRunner = batchInsertRunner;
        this.hikariDataSource = hikariDataSource;
        this.executorService = Executors.newFixedThreadPool(6); // Redis 조회는 CPU보다는 네트워크 I/O가 중요 → 6개 스레드 사용

        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    @Override
    public void processDataInBatches() {
        while (true) {
            int totalFetched = 0;

            while (totalFetched < MAX_BATCH_PER_CYCLE) {
                int fetched = fetchDataFromRedis(redisTemplate, MAX_BATCH_PER_CYCLE - totalFetched)
                        + fetchDataFromRedis(secondRedisTemplate, MAX_BATCH_PER_CYCLE - totalFetched);
                totalFetched += fetched;

                if (fetched == 0) {
                    break; // 더 이상 가져올 데이터가 없으면 종료
                }
            }

            log.info("Fetched {} records. Starting DB insert...", totalFetched);

            if (totalFetched == 0) {
                log.info("No more data to process. Exiting...");
                break;
            }

            insertDataFromQueue();
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.error("Batch insert interrupted = {}", e.getMessage());
        }

        log.info("All data inserted successfully!");
    }

    private void insertDataFromQueue() {
        AtomicInteger remainingBatches = new AtomicInteger(queue.size());

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                while (!queue.isEmpty()) {
                    try {
                        waitForConnection();

                        List<TbDtfHrasAuto> batch = queue.poll(1, TimeUnit.SECONDS);
                        if (batch != null && !batch.isEmpty()) {
                            batchInsertRunner.runBatchInsert(batch, THREAD_COUNT);
                            remainingBatches.decrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("Error inserting batch into DB", e);
                    }
                }
            });
        }

        while (remainingBatches.get() > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void waitForConnection() {
        while (true) {
            try (Connection connection = hikariDataSource.getConnection()) {
                if (connection != null) {
                    return;
                }
            } catch (SQLException e) {
                log.warn("현재 사용 가능한 DB 커넥션이 없습니다. 대기 중...");
            }
            try {
                TimeUnit.MILLISECONDS.sleep(3000);  // 3초 대기 후 다시 체크
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private int fetchDataFromRedis(RedisTemplate<String, String> redisTemplate, int remainingCapacity) {
        Set<String> keys = scanKeys(redisTemplate, REDIS_KEY_PATTERN);
        if (keys.isEmpty()) {
            return 0;
        }

        List<String> keyList = new ArrayList<>(keys);
        int totalFetched = 0;

        for (int i = 0; i < keyList.size() && totalFetched < remainingCapacity; i += BATCH_KEY_SIZE) {
            int endIdx = Math.min(i + BATCH_KEY_SIZE, keyList.size());
            List<String> subKeys = keyList.subList(i, endIdx);

            if (totalFetched >= remainingCapacity) {
                break;
            }

            try {
                List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String key : subKeys) {
                        connection.listCommands().lRange(key.getBytes(), 0, REDIS_FETCH_LIMIT - 1);
                    }
                    return null;
                });

                for (Object result : results) {
                    if (result instanceof List) {
                        List<TbDtfHrasAuto> batchData = ((List<String>) result).stream()
                                .map(this::deserializeJson)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        if (totalFetched + batchData.size() > remainingCapacity) {
                            batchData = batchData.subList(0, remainingCapacity - totalFetched);
                            totalFetched = remainingCapacity; // 정확히 3,000,000 유지
                        } else {
                            totalFetched += batchData.size();
                        }

                        if (!batchData.isEmpty()) {
                            queue.put(batchData); // 바로 큐에 넣음
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching data from Redis: {}", e.getMessage());
            }

            redisTemplate.delete(subKeys); // 사용한 키 삭제

            if (totalFetched >= remainingCapacity) {
                break;
            }
        }

        return totalFetched;
    }

    private Set<String> scanKeys(RedisTemplate<String, String> redisTemplate, String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(BATCH_KEY_SIZE).build();
        Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(redisConnection -> redisConnection.scan(options));

        if (cursor != null) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
            cursor.close();
        }

        return keys;
    }

    private TbDtfHrasAuto deserializeJson(String jsonData) {
        try {
            return objectMapper.readValue(jsonData, TbDtfHrasAuto.class);
        } catch (Exception e) {
            log.error("Failed to deserialize HRAS data: {}", e.getMessage());
            return null;
        }
    }
}
