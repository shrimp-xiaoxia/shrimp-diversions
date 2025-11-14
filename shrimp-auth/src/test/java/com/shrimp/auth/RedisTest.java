package com.shrimp.auth;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@Slf4j
public class RedisTest {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Test
    public void setKey(){
        redisTemplate.opsForValue().set("shrimp_id_generator",1000);
    }

}
