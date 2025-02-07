package com.example.vm1.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TbDtfHrasAutoPk implements Serializable {

    private String csId;

    private LocalDateTime pdctDt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TbDtfHrasAutoPk)) return false;
        TbDtfHrasAutoPk that = (TbDtfHrasAutoPk) o;
        return Objects.equals(csId, that.csId)
                && Objects.equals(pdctDt, that.pdctDt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(csId, pdctDt);
    }

    @Override
    public String toString() {
        return "TbDtfHrasAutoPk{" +
                "csId='" + csId + '\'' +
                ", pdctDt=" + pdctDt +
                '}';
    }
}

