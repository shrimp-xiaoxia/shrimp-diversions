package com.shrimp.oss.storage.Imlp;

import com.shrimp.framework.common.exception.BizException;
import com.shrimp.oss.config.MinioProperties;
import com.shrimp.oss.enums.ResponseCodeEnum;
import com.shrimp.oss.storage.FileStorage;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
public class MinioFileStorage implements FileStorage {
    
    @Resource
    private MinioClient minioClient;
    @Resource
    private MinioProperties minioProperties;
    
    @SneakyThrows
    @Override
    public String uploadFile(MultipartFile file, String buckerName) {
//        参数检查
        if(file == null || file.getSize() == 0){
           throw new BizException(ResponseCodeEnum.INVALID_PARAM);
        }
//        获取文件名和contentType
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
//      使用uuid和文件名的后缀来得到新的名称
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String key = UUID.randomUUID().toString().replaceAll("-", "");
        String newName = String.format("%s%s", key, suffix);
//        上传文件
        log.info("==> 开始上传文件到minion");
        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(buckerName)
                        .contentType(contentType)
                        .stream(file.getInputStream(),file.getSize(),-1)
                        .object(newName)
                .build());
//        拼接URL放回给客户端
        String url = String.format("%s/%s/%s",minioProperties.getEndPoint(),buckerName,newName);
        log.info("==> 上传文件至 Minio 成功，访问路径: {}", url);
        return url;
    }
}
