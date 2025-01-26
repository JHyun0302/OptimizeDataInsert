package com.example.vm3.scheduler;

import com.example.vm3.redis.RedisStreamConsumer;
import com.example.vm3.service.DummyDataInsertService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final RedisStreamConsumer redisStreamConsumer;

    private final DummyDataInsertService dummyDataInsertService;

    @Value("${spring.application.role}")
    private String role;

    @Scheduled(fixedRate = 5000)
    public void scheduleTask() {
        if ("producer".equals(role)) {
            dummyDataInsertService.insertDummyData(); // 데이터 생성
        } else if ("consumer".equals(role)) {
            redisStreamConsumer.consumeStream(); // 데이터 소비
        }
    }
}
