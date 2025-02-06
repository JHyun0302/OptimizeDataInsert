package com.example.vm1.scheduler;

import com.example.vm1.repository.TbDtfHrasAutoRepository;
import com.example.vm1.thread.ConsumerTask;
import com.example.vm1.thread.ProducerTask;
import com.example.vm1.thread.UpdateTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class DummyDataScheduler {

    private final BlockingQueue<UpdateTask>[] queue  = new BlockingQueue[10];

    private final TbDtfHrasAutoRepository repository;

    private final List<Thread> consumerThreads = new ArrayList<>();

    private final ObjectMapper objectMapper;

    private final RedisTemplate<String, String> redisTemplate;

    private final MeterRegistry meterRegistry;

    public static int THREAD_POOL = 10;

    // Producer & Consumer 스레드 풀 생성 (10개 스레드 유지)
    private final ExecutorService producerThreadPool = Executors.newFixedThreadPool(THREAD_POOL);
    private final ExecutorService consumerThreadPool = Executors.newFixedThreadPool(THREAD_POOL);

    public DummyDataScheduler(TbDtfHrasAutoRepository repository, ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        initializeQueues();
        startConsumers();
    }

    private void initializeQueues() {
        for (int i = 0; i < THREAD_POOL; i++) {
            queue[i] = new LinkedBlockingQueue<>();
        }
    }
    @Scheduled(fixedRate = 10000)
    public void scheduleInsertDummyData() {
        log.info("🔥 scheduleInsertDummyData() 실행됨 - Producer 생성 시작");

        for (int i = 0; i < THREAD_POOL; i++) {
            final int threadIndex = i;
            producerThreadPool.submit(() -> new ProducerTask(threadIndex, queue[threadIndex], repository, redisTemplate, objectMapper, meterRegistry).run());
        }
    }

    private void startConsumers() {
        for (int i = 0; i < THREAD_POOL; i++) {
            final int threadIndex = i;
            consumerThreadPool.submit(() -> new ConsumerTask(queue[threadIndex], objectMapper, redisTemplate).run());
        }
    }
}
