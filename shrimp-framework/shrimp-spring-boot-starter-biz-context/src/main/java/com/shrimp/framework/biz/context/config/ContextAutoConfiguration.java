package com.shrimp.framework.biz.context.config;

import com.shrimp.framework.biz.context.filter.HeaderUserId2Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextAutoConfiguration {
    @Bean
    public HeaderUserId2Filter getHeaderUserId2Filter() {
        return new HeaderUserId2Filter();
    }
}
