package com.example.vm2.start;

import com.example.vm2.Thread.MultiThreadBatchInsertRunner;
import com.example.vm2.entity.TbDtfHrasAuto;
import com.example.vm2.redis.GetDataFromRedis;
import com.example.vm2.redis.RedisInsertService;
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
public class DataInsertMain {

    private final GetDataFromRedis getDataFromRedis;

    private final RedisInsertService redisInsertService;

    private final MultiThreadBatchInsertRunner multiThreadBatchInsertRunner;

    private final Timer timer;

    public DataInsertMain(GetDataFromRedis getDataFromRedis,  RedisInsertService redisInsertService, MultiThreadBatchInsertRunner multiThreadBatchInsertRunner, MeterRegistry meterRegistry) {
        this.getDataFromRedis = getDataFromRedis;
        this.redisInsertService = redisInsertService;
        this.multiThreadBatchInsertRunner = multiThreadBatchInsertRunner;
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    //    @EventListener(ApplicationReadyEvent.class)
//    public void scheduleInsertDummyData() {
//        long startTime = System.currentTimeMillis();
//        for (int i = 0; i < 60; i++) {
//            redisInsertService.saveHrasDataInRedis();
//        }
//        long endTime = System.currentTimeMillis();
//
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//        String startTimeFormatted = sdf.format(new Date(startTime));
//        String endTimeFormatted = sdf.format(new Date(endTime));
//
//        log.info("StartTime: {}, EndTime: {}", startTimeFormatted, endTimeFormatted);
//    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    @EventListener(ApplicationReadyEvent.class)
    public void InsertDummyDataInDataBase() {
        long startTime = System.currentTimeMillis();
        timer.record(() -> {
            for (int vmIndex = 5; vmIndex < 10; vmIndex++) {
                List<TbDtfHrasAuto> dataList = getDataFromRedis.getData(vmIndex);
//                List<TbDtfHrasAuto> dataList = getDataFromRedis.getData(vmIndex, 30);
                multiThreadBatchInsertRunner.runBatchInsert(dataList);
            }
        });
        long endTime = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String startTimeFormatted = sdf.format(new Date(startTime));
        String endTimeFormatted = sdf.format(new Date(endTime));

        log.info("StartTime: {}, EndTime: {}", startTimeFormatted, endTimeFormatted);
    }
}
