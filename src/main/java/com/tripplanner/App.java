package com.tripplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application entry point.
 * 
 * Note: @EnableScheduling is now in SchedulingConfig.java
 * to properly configure the primary TaskScheduler bean.
 */
@SpringBootApplication
@EnableAsync
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

