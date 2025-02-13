package com.example.vm1.repository;

import com.example.vm1.entity.TbDtfHrasAuto;
import com.example.vm1.entity.TbDtfHrasAutoPk;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.driver.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * R2dbc
 */
@Repository
public interface TbDtfHrasAutoRepository extends ReactiveCrudRepository<TbDtfHrasAuto, String> {

    @Query("INSERT INTO tb_dtf_hras_auto " +
            "(cs_id, pdct_dt, project_id, name, river_name, river_reach, river_code, wtlv_val, flow_val, vel_val) " +
            "VALUES (:csId, :pdctDt, :projectId, :name, :riverName, :riverReach, :riverCode, :wtlvVal, :flowVal, :velVal)")
    Mono<Void> insertAuto(
            @Param("csId") String csId,
            @Param("pdctDt") LocalDateTime pdctDt,
            @Param("projectId") Long projectId,
            @Param("name") String name,
            @Param("riverName") String riverName,
            @Param("riverReach") String riverReach,
            @Param("riverCode") Long riverCode,
            @Param("wtlvVal") Long wtlvVal,
            @Param("flowVal") Long flowVal,
            @Param("velVal") Long velVal
    );

    @Query("DELETE FROM tb_dtf_hras_auto WHERE cs_id = :csId AND pdct_dt = :pdctDt")
    Mono<Void> deleteByCompositeKey(@Param("csId") String csId, @Param("pdctDt") LocalDateTime pdctDt);
}

