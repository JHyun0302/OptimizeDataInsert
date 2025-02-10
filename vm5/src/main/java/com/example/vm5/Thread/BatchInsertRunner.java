package com.example.vm5.Thread;

import com.example.vm5.entity.TbDtfHrasAuto;

import java.util.List;

public interface BatchInsertRunner {
    void runBatchInsert(List<TbDtfHrasAuto> dataList);
}
