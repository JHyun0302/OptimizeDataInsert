package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.service.BatchInsertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiThreadBatchInsertService {

    private final BatchInsertService batchInsertService;

    @Async("taskExecutor")  // 비동기 실행 (Thread Pool 사용)
    public void processBatch(List<TbDtfHrasAuto> batch, int batchSize) {
        log.info("Thread: {} processing {} records", Thread.currentThread().getName(), batch.size());
        batchInsertService.batchInsert(batch, batchSize);
    }
}
