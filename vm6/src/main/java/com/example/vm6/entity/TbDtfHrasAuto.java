package com.example.vm6.entity;

import jakarta.persistence.*;
import lombok.*;
import com.example.vm6.entity.common.BaseTimeEntity;

/**
 * 복합키 : CS_ID, PDCT_DT
 * VARCHAR2 -> String
 * NUMBER -> Long / Double / BigDecimal
 */
@Entity
@Table(name = "TB_DTF_HRAS_AUTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbDtfHrasAuto extends BaseTimeEntity {

    // 복합키 (CS_ID, PDCT_DT)
    @EmbeddedId
    private TbDtfHrasAutoPk pk;

    @Column(name = "PROJECT_ID", nullable = false)
    private Long projectId;

    @Column(name = "NAME", nullable = false, length = 30)
    private String name;

    @Column(name = "RIVER_NAME", nullable = false, length = 30)
    private String riverName;

    @Column(name = "RIVER_REACH", nullable = false, length = 30)
    private String riverReach;

    @Column(name = "RIVER_CODE", nullable = false)
    private Long riverCode;

    @Column(name = "WTLV_VAL", nullable = false)
    private Long wtlvVal;

    @Column(name = "FLOW_VAL", nullable = false)
    private Long flowVal;

    @Column(name = "VEL_VAL", nullable = false)
    private Long velVal;

    @Override
    public String toString() {
        return "TbDtfHrasAuto{" +
                "pk=" + pk +
                ", projectId=" + projectId +
                ", name='" + name + '\'' +
                ", riverName='" + riverName + '\'' +
                ", riverReach='" + riverReach + '\'' +
                ", riverCode=" + riverCode +
                ", wtlvVal=" + wtlvVal +
                ", flowVal=" + flowVal +
                ", velVal=" + velVal +
                '}';
    }
}