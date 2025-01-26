package com.example.vm3.scheduler;

import com.example.vm3.redis.RedisInsertService;
import com.example.vm3.redis.SaveInDataBase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final RedisInsertService redisInsertService;

    private final SaveInDataBase saveInDataBase;

    @Value("${spring.application.role}")
    private String role;

    @Scheduled(fixedRate = 5000) // 10초마다 실행
    public void scheduleInsertDummyData() {
        if ("saveDataInRedis".equals(role)) {
            redisInsertService.saveHrasDataInRedis();
        } else if("saveDataInDataBase".equals(role)) {
            saveInDataBase.transferHrasDataToDB();
        }
    }
}
