package com.shrimp.oss.service.Impl;

import com.shrimp.framework.common.response.Response;
import com.shrimp.oss.constants.MinioBuckerConstants;
import com.shrimp.oss.service.FileService;
import com.shrimp.oss.storage.FileStorage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author ccb
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Resource
    private FileStorage fileStorage;
    @Override
    public Response<?> uploadFile(MultipartFile file) {
        String url = fileStorage.uploadFile(file, MinioBuckerConstants.BUCKET_NAME);
        return Response.success(url);
    }
}
