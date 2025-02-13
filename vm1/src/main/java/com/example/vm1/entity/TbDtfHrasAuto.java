package com.example.vm1.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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

    // R2DBC에서는 복합 키를 직접 관리해야 함
    @Transient // DB의 PK는 따로 처리, 이 필드는 직렬화에서만 사용
    private TbDtfHrasAutoPk pk;

    @Column("cs_id") // 개별 필드로 매핑
    private String csId;

    @Column("pdct_dt") // 개별 필드로 매핑
    private LocalDateTime pdctDt;

    private Long projectId;
    private String name;
    private String riverName;
    private String riverReach;
    private Long riverCode;
    private Long wtlvVal;
    private Long flowVal;
    private Long velVal;

    // 복합 키를 직접 관리하기 위한 메서드 추가
    public String getCompositeKey() {
        return csId + "_" + pdctDt.toString();
    }

    // JSON 직렬화/역직렬화 시 자동으로 PK 값 설정
    public void setPk(TbDtfHrasAutoPk pk) {
        this.pk = pk;
        if (pk != null) {
            this.csId = pk.getCsId();
            this.pdctDt = pk.getPdctDt();
        }
    }

    public TbDtfHrasAutoPk getPk() {
        return new TbDtfHrasAutoPk(csId, pdctDt);
    }
}
