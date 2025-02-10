package com.example.vm10.Thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);  // 최소 8개 스레드
        executor.setMaxPoolSize(16);  // 최대 16개 스레드
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("DB-Insert-");
        executor.initialize();
        return executor;
    }
}
