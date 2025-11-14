package com.shrimp.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.shrimp.user.biz.domain.mapper")
@EnableFeignClients("com.shrimp")
public class ShrimpUserBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpUserBizApplication.class, args);
    }
}
