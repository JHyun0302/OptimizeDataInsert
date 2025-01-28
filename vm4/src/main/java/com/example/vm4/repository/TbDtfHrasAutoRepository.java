package com.example.vm4.repository;

import com.example.vm4.entity.TbDtfHrasAuto;
import com.example.vm4.entity.TbDtfHrasAutoPk;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TbDtfHrasAutoRepository   extends JpaRepository<TbDtfHrasAuto, TbDtfHrasAutoPk> {

    @Transactional
    @Modifying
    @Query("UPDATE TbDtfHrasAuto t SET t.wtlvVal = :wtlvVal, t.flowVal = :flowVal, t.velVal = :velVal WHERE t.pk.csId = :csId AND t.pk.pdctDt = :pdctDt")
    void updateValues(@Param("wtlvVal") Long wtlvVal,
                      @Param("flowVal") Long flowVal,
                      @Param("velVal") Long velVal,
                      @Param("csId") String csId,
                      @Param("pdctDt") LocalDateTime pdctDt);

    @Query("SELECT t FROM TbDtfHrasAuto t WHERE TO_NUMBER(SUBSTR(t.pk.csId, 4)) BETWEEN :startId AND :endId")
    List<TbDtfHrasAuto> findByRange(@Param("startId") int startId, @Param("endId") int endId);
}
