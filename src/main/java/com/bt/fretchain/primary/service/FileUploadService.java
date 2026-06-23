package com.bt.fretchain.primary.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    public String uploadCloudinaryFile(MultipartFile file);
}
