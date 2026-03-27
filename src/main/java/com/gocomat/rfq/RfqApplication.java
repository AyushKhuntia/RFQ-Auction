package com.gocomat.rfq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RfqApplication {
    public static void main(String[] args) {
        SpringApplication.run(RfqApplication.class, args);
    }
}
