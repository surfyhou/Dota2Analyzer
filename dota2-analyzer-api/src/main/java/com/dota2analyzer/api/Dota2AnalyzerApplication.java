package com.dota2analyzer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Dota2AnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(Dota2AnalyzerApplication.class, args);
    }
}
