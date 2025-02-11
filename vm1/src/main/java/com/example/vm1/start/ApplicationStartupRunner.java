package com.example.vm1.start;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationStartupRunner {

    private final TimeRecord timeRecord;

//    @EventListener(ApplicationReadyEvent.class)
//    public void scheduleInsertDummyData() {
//        timeRecord.insertDummyDataInRedis();
//    }

    @EventListener(ApplicationReadyEvent.class)
    public void InsertDummyDataInDataBase() {
        timeRecord.insertDummyDataInDataBase();
    }
}
