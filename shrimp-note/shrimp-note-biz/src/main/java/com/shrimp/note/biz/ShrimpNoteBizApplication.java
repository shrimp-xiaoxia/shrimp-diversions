package com.shrimp.note.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author ccb
 */
@SpringBootApplication
@MapperScan("com.shrimp.note.biz.domain.mapper")
@EnableFeignClients("com.shrimp")
public class ShrimpNoteBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpNoteBizApplication.class, args);
    }
}
