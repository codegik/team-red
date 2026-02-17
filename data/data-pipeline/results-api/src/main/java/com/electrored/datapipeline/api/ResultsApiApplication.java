package com.electrored.datapipeline.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ResultsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultsApiApplication.class, args);
    }
}

