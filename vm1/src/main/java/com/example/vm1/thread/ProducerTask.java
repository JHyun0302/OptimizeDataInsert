package com.example.vm1.thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.repository.TbDtfHrasAutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static com.example.vm1.scheduler.DummyDataScheduler.THREAD_POOL;

@Slf4j
public class ProducerTask implements Runnable {

    private final BlockingQueue<UpdateTask> queue;

    private static final String REDIS_KEY = "hras-data";

    private final TbDtfHrasAutoRepository repository;

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    private final int threadIndex;

    @Value("${spring.application.vm-index}")  // VM의 고유 인덱스 (1~16)
    private int vmIndex;

    @Value("${spring.application.total-vms}") // 총 VM 개수
    private int totalVms;

    public ProducerTask(int threadIndex, BlockingQueue<UpdateTask> queue, TbDtfHrasAutoRepository repository, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.threadIndex = threadIndex;
        this.queue = queue;
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Transactional
    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    @Override
    public void run() {
        //startId, endId 계산
        Result result = getResult(threadIndex);
        //Redis Key
        String redisKey = REDIS_KEY + ":" + vmIndex;

        log.info("🔥 Producer {} - Redis Key: {}, startId: {}, endId: {}", threadIndex, redisKey, result.startId(), result.endId());

        timer.record(() -> {
            // Redis에서 해당 키의 전체 데이터를 읽음 (JSON 문자열 목록)
            List<String> jsonRecords = redisTemplate.opsForList().range(redisKey, result.startId(), result.endId());
            if (jsonRecords == null || jsonRecords.isEmpty()) {
                log.warn("No records nfoud in Redis for range {} to {} (key: {})", result.startId(), result.endId(), redisKey);
                return;
            }

            // JSON 문자열을 TbDtfHrasAuto 객체로 변환
            List<TbDtfHrasAuto> records = new ArrayList<>();
            for (String json : jsonRecords) {
                try {
                    TbDtfHrasAuto record = objectMapper.readValue(json, TbDtfHrasAuto.class);
                    records.add(record);
                } catch (Exception e) {
                    log.error("Failed to deserialize JSON: {}", json, e);
                }
            }

            try {
                // 1) DB 업데이트 수행
                Long randomWtlv = (long) (Math.random() * 100 + 1);
                Long randomFlow = (long) (Math.random() * 100 + 1);
                Long randomVel = (long) (Math.random() * 100 + 1);

                int updatedRows = repository.bulkUpdateByRange(randomWtlv, randomFlow, randomVel, result.startId() , result.endId());
                log.info("Bulk updated {} rows range {} to {}", updatedRows, result.startId(), result.endId());

                // 3) Consumer가 사용할 UpdateTask 객체를 큐에 put
                UpdateTask task = new UpdateTask(redisKey, records);
                queue.offer(task);

                log.info("Queued UpdateTask -> redisKey= {}", redisKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Result getResult(int threadIndex) {
        int totalData = 12_000_000; // 총 데이터 건수
        int dataPerVm = totalData / totalVms;
        int dataPerThread = dataPerVm / THREAD_POOL; // 각 VM 내에서 각 스레드가 처리할 데이터 개수

        int startId = (vmIndex * dataPerVm) + (threadIndex * dataPerThread) + 1;
        int endId = startId + dataPerThread - 1;

        // 마지막 VM이 남은 데이터를 처리하도록 보정
        if (vmIndex == totalVms - 1 && threadIndex == THREAD_POOL - 1) {
            endId = totalData; // 마지막 스레드가 남은 데이터 처리
        }

        return new Result(startId, endId);
    }

    private record Result(int startId, int endId) {
    }
}
