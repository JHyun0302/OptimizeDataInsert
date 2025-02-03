package com.example.vm7.scheduler;

import com.example.vm7.redis.RedisInsertService;
import com.example.vm7.redis.SaveInDataBase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final RedisInsertService readFromRedisAndInsertToDB;

    private final SaveInDataBase saveInDataBase;

    @Value("${spring.application.vm-index}")  // VM의 고유 인덱스 (1~16)
    private int vmIndex;

    @Value("${spring.application.total-vms}") // 총 VM 개수
    private int totalVms;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void scheduleInsertDummyData() {
        int totalData = 12000000; // 총 데이터 건수
        int dataPerVm = totalData / totalVms;

        int startId = (vmIndex * dataPerVm) + 1;
        int endId = (vmIndex + 1) * dataPerVm;
        if (vmIndex == totalVms - 1) { // 마지막 VM 처리
            endId += totalData % totalVms;
        }

        if (vmIndex == 0) {
            // Redis 데이터를 읽고 DB에 Insert
            saveInDataBase.readFromRedisAndInsertToDB();
        } else {
            // DB 데이터를 읽고 Update 후 Redis에 저장
            readFromRedisAndInsertToDB.updateDBAndSaveToRedis(startId, endId, batchSize, vmIndex);
        }
    }
}
