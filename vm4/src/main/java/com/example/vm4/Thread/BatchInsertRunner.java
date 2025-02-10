package com.example.vm4.Thread;

import com.example.vm4.entity.TbDtfHrasAuto;

import java.util.List;

public interface BatchInsertRunner {
    void runBatchInsert(List<TbDtfHrasAuto> dataList);
}
