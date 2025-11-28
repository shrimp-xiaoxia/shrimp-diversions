package com.shrimp.count.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author ccb
 */
@SpringBootApplication
@MapperScan("com.shrimp.count.biz.domain.mapper")
public class ShrimpCountBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpCountBizApplication.class, args);
    }
}
