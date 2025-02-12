package com.example.vm1.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 복합키 : CS_ID, PDCT_DT
 * VARCHAR2 -> String
 * NUMBER -> Long / Double / BigDecimal
 */
@Table("tb_dtf_hras_auto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbDtfHrasAuto {

    @Id
    private TbDtfHrasAutoPk pk;

    private Long projectId;
    private String name;
    private String riverName;
    private String riverReach;
    private Long riverCode;
    private Long wtlvVal;
    private Long flowVal;
    private Long velVal;
}
