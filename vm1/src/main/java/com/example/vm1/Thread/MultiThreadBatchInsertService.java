package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.repository.TbDtfHrasAutoRepository;
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

    private final TbDtfHrasAutoRepository repository;

    @Async("taskExecutor")  // 비동기 실행 (Thread Pool 사용)
    public void processBatch(List<TbDtfHrasAuto> batch, int batchSize) {
        repository.batchInsert(batch, batchSize);
//        batchInsertService.batchInsert(batch, batchSize);
    }
}
