package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.repository.TbDtfHrasAutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchInsertService {

    private final JdbcTemplate jdbcTemplate;

    private final TbDtfHrasAutoRepository repository;

    @Async("taskExecutor")  // 비동기 실행 (Thread Pool 사용)
    public void processBatch(List<TbDtfHrasAuto> batch, int batchSize) {
//        repository.batchInsert(batch, batchSize);
        batchInsert(batch, batchSize);
    }

    public void batchInsert(List<TbDtfHrasAuto> dataList, int batchSize) {
        String sql = "INSERT /*+ APPEND */ INTO TB_DTF_HRAS_AUTO " +
                "(CS_ID, PDCT_DT, PROJECT_ID, NAME, RIVER_NAME, RIVER_REACH, RIVER_CODE, WTLV_VAL, FLOW_VAL, VEL_VAL) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, dataList, batchSize, (PreparedStatement ps, TbDtfHrasAuto data) -> {
            ps.setString(1, data.getPk().getCsId());
            ps.setTimestamp(2, Timestamp.valueOf(data.getPk().getPdctDt()));
            ps.setLong(3, data.getProjectId());
            ps.setString(4, data.getName());
            ps.setString(5, data.getRiverName());
            ps.setString(6, data.getRiverReach());
            ps.setLong(7, data.getRiverCode());
            ps.setLong(8, data.getWtlvVal());
            ps.setLong(9, data.getFlowVal());
            ps.setLong(10, data.getVelVal());
        });

//        log.info("Inserted {} records into DB", dataList.size());
    }
}
