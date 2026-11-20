package com.shrimp.search.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author ccb
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "com.shrimp")
@MapperScan("com.shrimp.search.biz.domain.mapper")
public class ShrimpSearchBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpSearchBizApplication.class, args);
    }
}
