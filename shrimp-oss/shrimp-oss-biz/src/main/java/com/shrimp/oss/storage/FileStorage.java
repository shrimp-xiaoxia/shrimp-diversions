package com.shrimp.oss.storage;

import com.shrimp.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    String  uploadFile(MultipartFile file,String buckerName);
}
