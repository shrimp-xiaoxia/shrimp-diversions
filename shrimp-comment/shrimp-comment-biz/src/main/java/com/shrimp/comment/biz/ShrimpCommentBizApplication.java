package com.shrimp.comment.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@MapperScan("com.shrimp.comment.biz.domain.mapper")
@EnableRetry
@EnableFeignClients(basePackages = "com.shrimp")
public class ShrimpCommentBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpCommentBizApplication.class, args);
    }
}
