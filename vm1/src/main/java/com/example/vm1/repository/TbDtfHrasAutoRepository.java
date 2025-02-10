package com.example.vm1.repository;

import com.example.vm1.entity.TbDtfHrasAuto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class TbDtfHrasAutoRepository {

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcCall simpleJdbcCall;

    public TbDtfHrasAutoRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("proc_bulk_insert")
                .withSchemaName("SYSTEM")
                .declareParameters(
                        new SqlParameter("p_cs_id", Types.ARRAY),
                        new SqlParameter("p_pdct_dt", Types.ARRAY),
                        new SqlParameter("p_project_id", Types.ARRAY),
                        new SqlParameter("p_name", Types.ARRAY),
                        new SqlParameter("p_river_name", Types.ARRAY),
                        new SqlParameter("p_river_reach", Types.ARRAY),
                        new SqlParameter("p_river_code", Types.ARRAY),
                        new SqlParameter("p_wtlv_val", Types.ARRAY),
                        new SqlParameter("p_flow_val", Types.ARRAY),
                        new SqlParameter("p_vel_val", Types.ARRAY)
                );
    }

    public void batchInsert(List<TbDtfHrasAuto> dataList) {
        Map<String, Object> params = Map.of(
                "p_cs_id", dataList.stream().map(d -> d.getPk().getCsId()).toArray(String[]::new),
                "p_pdct_dt", dataList.stream().map(d -> d.getPk().getPdctDt().toString()).toArray(String[]::new),
                "p_project_id", dataList.stream().map(TbDtfHrasAuto::getProjectId).toList().toArray(new Long[0]),
                "p_name", dataList.stream().map(TbDtfHrasAuto::getName).toList().toArray(new String[0]),
                "p_river_name", dataList.stream().map(TbDtfHrasAuto::getRiverName).toArray(String[]::new),
                "p_river_reach", dataList.stream().map(TbDtfHrasAuto::getRiverReach).toArray(String[]::new),
                "p_river_code", dataList.stream().map(TbDtfHrasAuto::getRiverCode).toArray(Long[]::new),
                "p_wtlv_val", dataList.stream().map(TbDtfHrasAuto::getWtlvVal).toArray(Long[]::new),
                "p_flow_val", dataList.stream().map(TbDtfHrasAuto::getFlowVal).toArray(Long[]::new),
                "p_vel_val", dataList.stream().map(TbDtfHrasAuto::getVelVal).toList().toArray(new Long[0])
        );

        simpleJdbcCall.execute(params);
    }
}

