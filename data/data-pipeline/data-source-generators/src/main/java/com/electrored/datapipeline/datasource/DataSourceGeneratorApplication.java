package com.electrored.datapipeline.datasource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataSourceGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataSourceGeneratorApplication.class, args);
    }
}

