package com.example.vm6.scheduler;

import com.example.vm6.redis.RedisInsertService;
import com.example.vm6.redis.SaveInDataBase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final RedisInsertService redisInsertService;

    private final SaveInDataBase saveInDataBase;

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void scheduleInsertDummyData() {
        redisInsertService.saveHrasDataInRedis();
        saveInDataBase.transferHrasDataToDB();
    }
}
