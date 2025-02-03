package com.example.vm6.scheduler;

import com.example.vm6.service.UpdateQueryWithNoRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final UpdateQueryWithNoRedisService dataUpdateService;

    @Value("${spring.application.vm-index}")  // VM의 고유 인덱스 (1~16)
    private int vmIndex;

    @Value("${spring.application.total-vms}") // 총 VM 개수
    private int totalVms;

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void scheduleInsertDummyData() {
        int totalData = 12000000; // 총 데이터 건수
        int dataPerVm = totalData / totalVms;

        int startId = (vmIndex * dataPerVm) + 1;
        int endId = (vmIndex + 1) * dataPerVm;
        if (vmIndex == totalVms - 1) { // 마지막 VM 처리
            endId += totalData % totalVms;
        }

        dataUpdateService.updateQueryWithNoRedis(startId, endId);
    }
}
