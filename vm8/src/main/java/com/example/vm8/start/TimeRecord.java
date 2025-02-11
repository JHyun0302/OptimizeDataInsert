package com.example.vm8.start;

import com.example.vm8.Thread.BatchInsertRunner;
import com.example.vm8.redis.GetDataFromRedis;
import com.example.vm8.redis.RedisInsertService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class TimeRecord {

    private final GetDataFromRedis getDataFromRedis;

    private final RedisInsertService redisInsertService;

    private final BatchInsertRunner batchInsertRunner;

    public TimeRecord(GetDataFromRedis getDataFromRedis, RedisInsertService redisInsertService, @Qualifier("singleThreadBatchInsertRunner") BatchInsertRunner batchInsertRunner, MeterRegistry meterRegistry) {
        this.getDataFromRedis = getDataFromRedis;
        this.redisInsertService = redisInsertService;
        this.batchInsertRunner = batchInsertRunner;
    }


    public void insertDummyDataInRedis() {
        long startTime = System.currentTimeMillis();
        int batchIndex = 0;

        for (int i = 0; i < 60; i++) { // VM별 60개의 Redis 키 생성
            redisInsertService.saveHrasDataInRedis(batchIndex);
            batchIndex++; // 배치 인덱스 증가
        }

        long endTime = System.currentTimeMillis();
        timeTrace(startTime, endTime);
    }

    private static void timeTrace(long startTime, long endTime) {
        log.info("StartTime: {}, EndTime: {}",
                new SimpleDateFormat("HH:mm:ss").format(new Date(startTime)),
                new SimpleDateFormat("HH:mm:ss").format(new Date(endTime)));
    }
}
