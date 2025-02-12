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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchInsertService {

    private final JdbcTemplate jdbcTemplate;

    private final TbDtfHrasAutoRepository repository;

    @Async("taskExecutor")  // 비동기 실행 (Thread Pool 사용)
    public Integer processBatch(List<TbDtfHrasAuto> batch, int batchSize) {
//        repository.batchInsert(batch, batchSize);
        return batchInsert(batch, batchSize);
    }

    /**
     * 테이블 8개에 인서트 진행
     */
    public int batchInsert(List<TbDtfHrasAuto> dataList, int batchSize) {
        int totalInserted = 0;

        // 데이터를 8개의 테이블로 분배하여 저장
        Map<Integer, List<TbDtfHrasAuto>> shardedData = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            shardedData.put(i, new ArrayList<>());
        }

        // 각 데이터의 projectId를 기반으로 Sharding
        for (TbDtfHrasAuto data : dataList) {
            int shardIndex = (int) (data.getProjectId() % 8);
            shardedData.get(shardIndex).add(data);
        }

        // 각 테이블에 대해 병렬로 Insert 수행
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            final int tableIndex = i;
            List<TbDtfHrasAuto> batch = shardedData.get(tableIndex);

            if (!batch.isEmpty()) {
                futures.add(executor.submit(() -> insertIntoShardedTable(batch, tableIndex, batchSize)));
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.error("Batch insert interrupted", e);
        }

        for (Future<Integer> future : futures) {
            try {
                totalInserted += future.get();
            } catch (Exception e) {
                log.error("Error while inserting batch", e);
            }
        }

        return totalInserted;
    }

    private int insertIntoShardedTable(List<TbDtfHrasAuto> batch, int tableIndex, int batchSize) {
        String tableName = "TB_DTF_HRAS_AUTO_" + tableIndex;
        String sql = "INSERT /*+ APPEND_VALUES */ INTO " + tableName + " " +
                "(CS_ID, PDCT_DT, PROJECT_ID, NAME, RIVER_NAME, RIVER_REACH, RIVER_CODE, WTLV_VAL, FLOW_VAL, VEL_VAL) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int totalInserted = 0;

        for (int i = 0; i < batch.size(); i += batchSize) {
            int endIdx = Math.min(i + batchSize, batch.size());
            List<TbDtfHrasAuto> subBatch = batch.subList(i, endIdx);

            int[][] insertedRows = jdbcTemplate.batchUpdate(sql, subBatch, batchSize, (ps, data) -> {
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

            totalInserted += Arrays.stream(insertedRows).flatMapToInt(Arrays::stream).sum();
        }

        return totalInserted;
    }

//    public int batchInsert(List<TbDtfHrasAuto> dataList, int batchSize) {
//        String sql = "INSERT /*+ APPEND_VALUES */ INTO TB_DTF_HRAS_AUTO " +
//                "(CS_ID, PDCT_DT, PROJECT_ID, NAME, RIVER_NAME, RIVER_REACH, RIVER_CODE, WTLV_VAL, FLOW_VAL, VEL_VAL) " +
//                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//
//        int safeBatchSize = Math.min(batchSize, 1000); // 오라클 최대 바인딩 변수 초과 방지
//        int totalInserted = 0;
//
//        // 데이터 전체를 safeBatchSize 크기로 나누어 처리
//        for (int i = 0; i < dataList.size(); i += safeBatchSize) {
//            int endIdx = Math.min(i + safeBatchSize, dataList.size());
//            List<TbDtfHrasAuto> batch = dataList.subList(i, endIdx);
//
//            int[][] insertedRows = jdbcTemplate.batchUpdate(sql, batch, safeBatchSize, (ps, data) -> {
//                ps.setString(1, data.getPk().getCsId());
//                ps.setTimestamp(2, Timestamp.valueOf(data.getPk().getPdctDt()));
//                ps.setLong(3, data.getProjectId());
//                ps.setString(4, data.getName());
//                ps.setString(5, data.getRiverName());
//                ps.setString(6, data.getRiverReach());
//                ps.setLong(7, data.getRiverCode());
//                ps.setLong(8, data.getWtlvVal());
//                ps.setLong(9, data.getFlowVal());
//                ps.setLong(10, data.getVelVal());
//            });
//
//            totalInserted += Arrays.stream(insertedRows).flatMapToInt(Arrays::stream).sum();
//        }
//
//        return totalInserted;
//    }
}
