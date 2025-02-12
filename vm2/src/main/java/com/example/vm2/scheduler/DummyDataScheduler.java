package com.example.vm2.scheduler;

import com.example.vm2.redis.RedisStreamConsumer;
import com.example.vm2.service.DummyDataInsertService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final RedisStreamConsumer redisStreamConsumer;

    private final DummyDataInsertService dummyDataInsertService;

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void scheduleInsertDummyData() {
        redisStreamConsumer.consumeStream();
        dummyDataInsertService.insertDummyData();
    }
}
