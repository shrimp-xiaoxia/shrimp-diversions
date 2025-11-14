package com.shrimp.framework.biz.context.config;

import com.shrimp.framework.biz.context.interceptor.FeignRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignContextAutoConfiguration {

    @Bean
    public FeignRequestInterceptor getFeignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }
}
