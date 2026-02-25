package com.shrimp.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.shrimp.data.align.domain.mapper")
@EnableFeignClients(basePackages = "com.shrimp")
public class ShrimpDataAlignApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpDataAlignApplication.class, args);
    }
}
