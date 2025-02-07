package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
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
public class MultiThreadBatchInsertRunner {

    private final MultiThreadBatchInsertService multiThreadBatchInsertService;

    private static final int THREAD_COUNT = 4;

    @Value("${spring.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    public void runBatchInsert(List<TbDtfHrasAuto> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.info("No data to insert.");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        log.info("Starting parallel batch insert for {} records", dataList.size());

        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<TbDtfHrasAuto> batch = dataList.subList(i, end);

            executor.submit(() -> multiThreadBatchInsertService.processBatch(batch, batchSize));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.error("Batch insert interrupted", e);
        }

        log.info("Parallel batch insert completed!");
    }
}
