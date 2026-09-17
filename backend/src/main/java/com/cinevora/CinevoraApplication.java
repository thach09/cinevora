package com.cinevora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CinevoraApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinevoraApplication.class, args);
    }
}
