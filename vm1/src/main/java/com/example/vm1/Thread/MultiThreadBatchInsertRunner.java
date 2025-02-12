package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiThreadBatchInsertRunner implements BatchInsertRunner {

    private final BatchInsertService batchInsertService;

    private static final int THREAD_COUNT = 8;

    public int runBatchInsert(List<TbDtfHrasAuto> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.info("No data to insert.");
            return 0;
        }

        int totalSize = dataList.size();
        int chunkSize = totalSize / THREAD_COUNT; // 스레드별 할당할 데이터 개수
        int remaining = totalSize % THREAD_COUNT; // 남는 데이터 처리

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Integer>> futures = new ArrayList<>();

        log.info("Starting parallel batch insert for {} records with {} threads. chunkSize, remaining = {}, {}", totalSize, THREAD_COUNT, chunkSize, remaining);

        int startIdx = 0;

        for (int i = 0; i < THREAD_COUNT; i++) {
            int endIdx = startIdx + chunkSize + (i < remaining ? 1 : 0);
            List<TbDtfHrasAuto> batch = dataList.subList(startIdx, endIdx);
            startIdx = endIdx;

            futures.add(executor.submit(() -> batchInsertService.processBatch(batch, 1000)));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.error("Batch insert interrupted", e);
        }

        int totalInserted = 0;

        // 모든 Future 결과 확인
        for (Future<Integer> future : futures) {
            try {
                log.info("Inserted {} records into DB", future.get());
            } catch (Exception e) {
                log.error("Error while inserting batch", e);
            }
        }

        return totalInserted; // 총 Insert된 개수 반환
    }
}
