package com.example.licenseplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LicensePlateApplication {
    public static void main(String[] args) {
        SpringApplication.run(LicensePlateApplication.class, args);
    }
}