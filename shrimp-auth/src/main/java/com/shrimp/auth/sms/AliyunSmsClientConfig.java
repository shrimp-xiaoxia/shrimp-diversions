package com.shrimp.auth.sms;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AliyunSmsClientConfig {
    @Resource
    private AliyunAccessKeyProperties aliyunAccessKeyProperties;

    @Bean
    public com.aliyun.dypnsapi20170525.Client smsClient(){
        try {
            com.aliyun.credentials.Client credential = new  com.aliyun.credentials.Client();
            com.aliyun.teaopenapi.models.Config config = new  com.aliyun.teaopenapi.models.Config().setCredential(credential);

            config.setEndpoint("dypnsapi.aliyuncs.com");
            config.setAccessKeyId(aliyunAccessKeyProperties.getAccessKeyId());
            config.setAccessKeySecret(aliyunAccessKeyProperties.getAccessKeySecret());

            return new  com.aliyun.dypnsapi20170525.Client(config);
        } catch (Exception e) {
            log.error("发送短信消息客户端失败");
            return null;
        }
    }
}
