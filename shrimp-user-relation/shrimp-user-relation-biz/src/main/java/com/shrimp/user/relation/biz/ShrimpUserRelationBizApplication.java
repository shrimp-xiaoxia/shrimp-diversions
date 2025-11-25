package com.shrimp.user.relation.biz;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author ccb
 */
@SpringBootApplication
@MapperScan("com.shrimp.user.relation.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.shrimp")
public class ShrimpUserRelationBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShrimpUserRelationBizApplication.class, args);
    }
}
