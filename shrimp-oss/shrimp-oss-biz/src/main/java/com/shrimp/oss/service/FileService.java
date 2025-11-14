package com.shrimp.oss.service;

import com.shrimp.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    Response<?> uploadFile(MultipartFile file);
}
