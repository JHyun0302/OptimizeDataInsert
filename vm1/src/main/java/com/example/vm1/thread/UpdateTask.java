package com.example.vm1.thread;

import com.example.vm1.entity.TbDtfHrasAuto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class UpdateTask {
    private final String redisKey;

    private final List<TbDtfHrasAuto> records;
}
