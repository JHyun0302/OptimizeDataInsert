package com.example.vm7.service;

import com.example.vm7.entity.TbDtfHrasAuto;
import com.example.vm7.entity.TbDtfHrasAutoPk;
import com.example.vm7.repository.TbDtfHrasAutoRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
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
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DummyDataInsertService {

    private final TbDtfHrasAutoRepository repository;

    private static final Random RANDOM = new Random();

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Timed(value = "dummy_data.insert.time", description = "Time taken to insert dummy data")
    @Counted(value = "dummy_data.insert.count", description = "Number of times dummy data is inserted")
    @Transactional
    public void insertDummyData() {
        try {
            List<TbDtfHrasAuto> dummyData = new ArrayList<>();

            for (int i = 0; i < batchSize; i++) {
                TbDtfHrasAuto record = generateDummyRecord(i);
                dummyData.add(record);

                // 배치 크기마다 flush 및 clear 수행
                if (dummyData.size() % batchSize == 0) { // batch_size와 동일하게 설정
                    repository.saveAll(dummyData); // INSERT 실행
                    repository.flush();            // 강제 Flush
                    dummyData.clear();             // 1차 캐시 Clear
                }
            }

            // 남은 데이터 처리
            if (!dummyData.isEmpty()) {
                repository.saveAll(dummyData);
                repository.flush();
                dummyData.clear();
            }

            log.info("Inserted {} dummy records", batchSize);
        } catch (Exception e) {
            log.error("Error occurred during dummy data insert: {}", e.getMessage());
            throw new RuntimeException();
        }
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
