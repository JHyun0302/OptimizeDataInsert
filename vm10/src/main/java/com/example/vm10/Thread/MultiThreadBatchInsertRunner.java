package com.example.vm10.Thread;

import com.example.vm10.Thread.MultiThreadBatchInsertService;
import com.example.vm10.entity.TbDtfHrasAuto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiThreadBatchInsertRunner implements BatchInsertRunner {

    private final MultiThreadBatchInsertService multiThreadBatchInsertService;

    private static final int THREAD_COUNT =  Runtime.getRuntime().availableProcessors();

    @Value("${spring.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    public void runBatchInsert(List<TbDtfHrasAuto> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.info("No data to insert.");
            return;
        }

        int totalSize = dataList.size();
        int chunkSize = totalSize / THREAD_COUNT; // ✅ 각 스레드에 할당할 데이터 개수
        int remaining = totalSize % THREAD_COUNT; // ✅ 나누어 떨어지지 않는 경우 추가 처리

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        log.info("Starting parallel batch insert for {} records, thread_count={}", totalSize, THREAD_COUNT);

        int startIdx = 0;

        for (int i = 0; i < THREAD_COUNT; i++) {
            int endIdx = startIdx + chunkSize + (i < remaining ? 1 : 0); // ✅ 데이터가 균등하게 분배되도록 설정
            List<TbDtfHrasAuto> batch = dataList.subList(startIdx, endIdx);
            startIdx = endIdx;

            executor.submit(() -> multiThreadBatchInsertService.processBatch(batch, batchSize));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.error("Batch insert interrupted", e);
        }
    }
}
