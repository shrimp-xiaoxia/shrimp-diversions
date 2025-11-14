package com.shrimp.oss.factory;

import com.shrimp.oss.storage.FileStorage;
import com.shrimp.oss.storage.Imlp.AliyunOSSFileStorage;
import com.shrimp.oss.storage.Imlp.MinioFileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class FileStorageFactory {
    @Value("${storage.type}")
    private String storageType;
    @Bean
    @RefreshScope
    public FileStorage getFileStorage() {
        if("aliyunOss".equals(storageType)){
            return new AliyunOSSFileStorage();
        }else if("minio".equals(storageType)){
            return new MinioFileStorage();
        }
        throw new IllegalArgumentException("不可用的存储类型");
    }
}
