package com.example.vm10.service;

import com.example.vm10.entity.TbDtfHrasAuto;
import com.example.vm10.entity.TbDtfHrasAutoPk;
import com.example.vm10.redis.RedisStreamService;
import com.example.vm10.repository.TbDtfHrasAutoRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@Transactional(readOnly = true)
public class DummyDataInsertService {

    private final RedisStreamService redisStreamService;

    private final TbDtfHrasAutoRepository repository;

    private final Counter successCounter;

    private final Counter failureCounter;

    private final Timer timer;

    private static final Random RANDOM = new Random();

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    public DummyDataInsertService(RedisStreamService redisStreamService, TbDtfHrasAutoRepository repository, MeterRegistry meterRegistry) {
        this.redisStreamService = redisStreamService;
        this.repository = repository;
        this.successCounter = meterRegistry.counter("dummy_data.insert.success");
        this.failureCounter = meterRegistry.counter("dummy_data.insert.failure");
        this.timer = meterRegistry.timer("dummy_data.insert.timer");
    }

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    @Transactional
    public void insertDummyData() {
        timer.record(() -> {
            try {
                List<TbDtfHrasAuto> dummyData = new ArrayList<>();

                for (int i = 0; i < batchSize; i++) {
                    TbDtfHrasAuto record = generateDummyRecord(i);
                    dummyData.add(record);

                    // 배치 크기마다 flush 및 clear 수행
                    if (dummyData.size() % batchSize == 0) { // batch_size와 동일하게 설정
                        dummyData.forEach(redisStreamService::writeToStream); // Redis에 저장
                        dummyData.clear();
                    }
                }

                // 남은 데이터 처리
                if (!dummyData.isEmpty()) {
                    dummyData.forEach(redisStreamService::writeToStream);
                }

                successCounter.increment();
            } catch (Exception e) {
                failureCounter.increment();
                throw new RuntimeException(e);
            }
        });
    }

    private TbDtfHrasAuto generateDummyRecord(int index) {
        String csId = "CS_" + (1000000 + index); // CS_ID 형식: 1000000부터 시작
        long projectId = 1000000 + RANDOM.nextInt(100000); // PROJECT_ID: 랜덤
        String name = "Project-" + index; // NAME
        String riverName = "River-" + (RANDOM.nextInt(5) + 1); // RIVER_NAME: River-1 ~ River-5
        String riverReach = "Reach-" + (RANDOM.nextInt(10) + 1); // RIVER_REACH: Reach-1 ~ Reach-10
        long riverCode = 1000 + RANDOM.nextInt(100); // RIVER_CODE
        long wtlvVal = (RANDOM.nextLong() * 500); // WTLV_VAL: 0~500
        long flowVal = (RANDOM.nextLong() * 200); // FLOW_VAL: 0~200
        long velVal = RANDOM.nextLong() * 5; // VEL_VAL: 0~5
        LocalDateTime pdctDt = LocalDateTime.now().minusDays(RANDOM.nextInt(30)); // PDCT_DT: 최근 30일 내 날짜

        TbDtfHrasAutoPk pk = new TbDtfHrasAutoPk(csId, pdctDt);

        return TbDtfHrasAuto.builder()
                .pk(pk)
                .projectId(projectId)
                .name(name)
                .riverName(riverName)
                .riverReach(riverReach)
                .riverCode(riverCode)
                .wtlvVal(wtlvVal)
                .flowVal(flowVal)
                .velVal(velVal)
                .build();
    }
}
