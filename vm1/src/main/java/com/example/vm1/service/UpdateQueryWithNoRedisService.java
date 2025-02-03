package com.example.vm1.service;

import com.example.vm1.repository.TbDtfHrasAutoRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UpdateQueryWithNoRedisService {

    private final TbDtfHrasAutoRepository repository;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    public UpdateQueryWithNoRedisService(TbDtfHrasAutoRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Transactional
    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    public void updateQueryWithNoRedis(int startId, int endId) {
        timer.record(() -> {
            Long randomWtlv = (long) (Math.random() * 100 + 1);
            Long randomFlow = (long) (Math.random() * 100 + 1);
            Long randomVel = (long) (Math.random() * 100 + 1);

            int updatedRows = repository.bulkUpdateByRange(randomWtlv, randomFlow, randomVel, startId, endId);

            log.info("Bulk updated {} rows for range {} to {}", updatedRows, startId, endId);
        });
    }
}