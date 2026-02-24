package com.tradeintel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TradeintelApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeintelApplication.class, args);
    }
}
