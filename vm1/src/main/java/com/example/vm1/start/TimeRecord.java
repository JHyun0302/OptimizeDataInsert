package com.example.vm1.start;

import com.example.vm1.Thread.BatchInsertRunner;
import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.Thread.DataBaseInsertService;
import com.example.vm1.redis.RedisInsertService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TimeRecord {

    private final DataBaseInsertService dataBaseInsertService;

    private final RedisInsertService redisInsertService;

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer timer;

    public TimeRecord(DataBaseInsertService dataBaseInsertService, RedisInsertService redisInsertService, @Qualifier("multiThreadBatchInsertRunner") BatchInsertRunner batchInsertRunner, MeterRegistry meterRegistry) {
        this.dataBaseInsertService = dataBaseInsertService;
        this.redisInsertService = redisInsertService;

        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
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

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void insertDummyDataInDataBase() {
        long startTime = System.currentTimeMillis();

        // Redis에서 일정 크기씩 데이터를 가져오고, 가져온 데이터를 즉시 DB에 Insert
        dataBaseInsertService.processDataInBatches();

        long endTime = System.currentTimeMillis();
        timeTrace(startTime, endTime);
    }

    private static void timeTrace(long startTime, long endTime) {
        log.info("StartTime: {}, EndTime: {}",
                new SimpleDateFormat("HH:mm:ss").format(new Date(startTime)),
                new SimpleDateFormat("HH:mm:ss").format(new Date(endTime)));
    }
}
