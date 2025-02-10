package com.example.vm8.Thread;

import com.example.vm8.entity.TbDtfHrasAuto;
import com.example.vm8.repository.TbDtfHrasAutoRepository;
import com.example.vm8.service.BatchInsertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SingleThreadBatchInsertRunner implements BatchInsertRunner {

    private final BatchInsertService batchInsertService;

    private final TbDtfHrasAutoRepository repository;

    @Value("${spring.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    public void runBatchInsert(List<TbDtfHrasAuto> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.info("No data to insert.");
            return;
        }

        log.info("Starting batch insert for {} records", dataList.size());

        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<TbDtfHrasAuto> batch = dataList.subList(i, end);

//            batchInsertService.batchInsert(batch, batchSize);
            try {
                repository.batchInsert(batch, batchSize);
            } catch (Exception e) {
                log.error("Error in processBatch: {}", e.getMessage(), e);
            }
        }

        log.info("Batch insert completed!");
    }
}

