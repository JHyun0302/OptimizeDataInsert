package com.example.vm6.Thread;

import com.example.vm6.entity.TbDtfHrasAuto;

import java.util.List;

public interface BatchInsertRunner {
    void runBatchInsert(List<TbDtfHrasAuto> dataList);
}
