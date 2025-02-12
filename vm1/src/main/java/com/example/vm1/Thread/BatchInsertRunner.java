package com.example.vm1.Thread;

import com.example.vm1.entity.TbDtfHrasAuto;

import java.util.List;

public interface BatchInsertRunner {
    int runBatchInsert(List<TbDtfHrasAuto> dataList);
}
