package com.example.vm2.scheduler;

import com.example.vm2.redis.RedisInsertService;
import com.example.vm2.redis.SaveInDataBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final RedisInsertService redisInsertService;

    private final SaveInDataBase saveInDataBase;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleInsertDummyData() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 60; i++) {
            redisInsertService.saveHrasDataInRedis();
        }
        long endTime = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String startTimeFormatted = sdf.format(new Date(startTime));
        String endTimeFormatted = sdf.format(new Date(endTime));

        log.info("StartTime: {}, EndTime: {}", startTimeFormatted, endTimeFormatted);
    }

    //@EventListener(ApplicationReadyEvent.class)
//    public void InsertDummyDataInDataBase() {
//        saveInDataBase.transferHrasDataToDB();
//    }
}
