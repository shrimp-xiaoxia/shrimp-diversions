package com.shrimp.count.biz.config;


import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ccb
 */
@Configuration
@RefreshScope
public class CountDBMqConsumerRateLimitConfig {
    @Value("${mq-consumer.follow-unfollow.rate-limit}")
    private double reteLimit;

    @Bean
    @RefreshScope
    public RateLimiter getReteLimit() {
        return RateLimiter.create(reteLimit);
    }
}
