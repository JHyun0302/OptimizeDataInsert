package com.example.vm8.repository;

import com.example.vm8.entity.TbDtfHrasAuto;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.driver.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TbDtfHrasAutoRepository {

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcCall simpleJdbcCall;

    private final DataSource dataSource;

    public TbDtfHrasAutoRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dataSource = dataSource;
        this.simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("proc_bulk_insert")
                .withSchemaName("SYSTEM");
    }

    public void batchInsert(List<TbDtfHrasAuto> dataList, int batchSize) {
        try (Connection conn = dataSource.getConnection()) {
            OracleConnection oracleConn = conn.unwrap(OracleConnection.class); // Oracle Connection unwrap

            ArrayDescriptor varcharArrayDesc = ArrayDescriptor.createDescriptor("SYSTEM.VARCHAR2_TABLE", oracleConn);
            ArrayDescriptor numberArrayDesc = ArrayDescriptor.createDescriptor("SYSTEM.NUMBER_TABLE", oracleConn);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // 날짜 포맷 지정
            // ✅ 5만 개씩 쪼개서 INSERT 수행
            for (int i = 0; i < dataList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, dataList.size());
                List<TbDtfHrasAuto> batch = dataList.subList(i, end);

                Map<String, Object> params = Map.of(
                        "p_cs_id", new ARRAY(varcharArrayDesc, oracleConn, batch.stream().map(d -> d.getPk().getCsId()).toArray(String[]::new)),
                        "p_pdct_dt", new ARRAY(varcharArrayDesc, oracleConn, batch.stream().map(d -> d.getPk().getPdctDt().format(dtf)).toArray(String[]::new)),
                        "p_project_id", new ARRAY(numberArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getProjectId).toArray(Long[]::new)),
                        "p_name", new ARRAY(varcharArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getName).toArray(String[]::new)),
                        "p_river_name", new ARRAY(varcharArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getRiverName).toArray(String[]::new)),
                        "p_river_reach", new ARRAY(varcharArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getRiverReach).toArray(String[]::new)),
                        "p_river_code", new ARRAY(numberArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getRiverCode).toArray(Long[]::new)),
                        "p_wtlv_val", new ARRAY(numberArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getWtlvVal).toArray(Long[]::new)),
                        "p_flow_val", new ARRAY(numberArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getFlowVal).toArray(Long[]::new)),
                        "p_vel_val", new ARRAY(numberArrayDesc, oracleConn, batch.stream().map(TbDtfHrasAuto::getVelVal).toArray(Long[]::new))
                );
                simpleJdbcCall.execute(params);
            }
        } catch (Exception e) {
            log.error("Error executing batchInsert, {}", e.getMessage());
        }
    }
}

