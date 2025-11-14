package com.shrimp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients("com.shrimp")
public class ShrimpAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpAuthApplication.class, args);
    }
}



