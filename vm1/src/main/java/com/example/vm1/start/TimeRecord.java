package com.example.vm1.start;

import com.example.vm1.Thread.MultiThreadBatchInsertRunner;
import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.redis.GetDataFromRedis;
import com.example.vm1.redis.RedisInsertService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TimeRecord {

    private final GetDataFromRedis getDataFromRedis;

    private final RedisInsertService redisInsertService;

    private final MultiThreadBatchInsertRunner multiThreadBatchInsertRunner;

    private final Timer timer;

    public TimeRecord(GetDataFromRedis getDataFromRedis, RedisInsertService redisInsertService, MultiThreadBatchInsertRunner multiThreadBatchInsertRunner, MeterRegistry meterRegistry) {
        this.getDataFromRedis = getDataFromRedis;
        this.redisInsertService = redisInsertService;
        this.multiThreadBatchInsertRunner = multiThreadBatchInsertRunner;
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void insertDummyDataInRedis() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 60; i++) {
            redisInsertService.saveHrasDataInRedis();
        }
        long endTime = System.currentTimeMillis();

        timeTrace(startTime, endTime);
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void insertDummyDataInDataBase() {
        long startTime = System.currentTimeMillis();
        timer.record(() -> {
            for (int vmIndex = 0; vmIndex < 10; vmIndex++) {
                List<TbDtfHrasAuto> dataList = getDataFromRedis.getData(vmIndex);
//                List<TbDtfHrasAuto> dataList = getDataFromRedis.getData(vmIndex, 30);
                multiThreadBatchInsertRunner.runBatchInsert(dataList);
            }
        });
        long endTime = System.currentTimeMillis();

        timeTrace(startTime, endTime);
    }

    private static void timeTrace(long startTime, long endTime) {
        log.info("StartTime: {}, EndTime: {}",
                new SimpleDateFormat("HH:mm:ss").format(new Date(startTime)),
                new SimpleDateFormat("HH:mm:ss").format(new Date(endTime)));
    }
}
