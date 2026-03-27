package com.gocomat.rfq;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class RfqApplication {

    @PostConstruct
    void setIstTimezone() {
        // Force IST so LocalDateTime.now() returns IST even on AWS (which defaults to UTC)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(RfqApplication.class, args);
    }
}
