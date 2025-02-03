package com.example.vm5.repository;

import com.example.vm5.entity.TbDtfHrasAuto;
import com.example.vm5.entity.TbDtfHrasAutoPk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TbDtfHrasAutoRepository   extends JpaRepository<TbDtfHrasAuto, TbDtfHrasAutoPk> {

    @Modifying
    @Transactional
    @Query(
            value = "UPDATE TB_DTF_HRAS_AUTO " +
                    "SET wtlv_val = :wtlvVal, " +
                    "    flow_val = :flowVal, " +
                    "    vel_val = :velVal " +
                    "WHERE TO_NUMBER(SUBSTR(cs_id, 4)) BETWEEN :startId AND :endId",
            nativeQuery = true)
    int bulkUpdateByRange(@Param("wtlvVal") Long wtlvVal,
                          @Param("flowVal") Long flowVal,
                          @Param("velVal") Long velVal,
                          @Param("startId") int startId,
                          @Param("endId") int endId);
}
