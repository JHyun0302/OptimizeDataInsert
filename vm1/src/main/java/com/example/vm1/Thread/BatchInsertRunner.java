package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;

import java.util.List;

public interface BatchInsertRunner {
    void runBatchInsert(List<TbDtfHrasAuto> dataList);
}
