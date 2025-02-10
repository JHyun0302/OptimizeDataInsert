package com.example.vm10.Thread;

import com.example.vm10.entity.TbDtfHrasAuto;

import java.util.List;

public interface BatchInsertRunner {
    void runBatchInsert(List<TbDtfHrasAuto> dataList);
}
