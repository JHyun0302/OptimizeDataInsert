package com.example.vm7.entity;

import com.example.vm7.entity.TbDtfHrasAutoPk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복합키 : CS_ID, PDCT_DT
 * VARCHAR2 -> String
 * NUMBER -> Long / Double / BigDecimal
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbDtfHrasAuto  {

    private TbDtfHrasAutoPk pk;

    private Long projectId;

    private String name;

    private String riverName;

    private String riverReach;

    private Long riverCode;

    private Long wtlvVal;

    private Long flowVal;

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