package com.corporate.vending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VendingApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendingApplication.class, args);
    }
}
