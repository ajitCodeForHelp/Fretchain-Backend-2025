package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.primary.service.FileUploadService;
import com.bt.fretchain.utils.ProjectUtils;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    protected Cloudinary cloudinary;

    @Override
    public String uploadCloudinaryFile(MultipartFile file) {

        try {
            String folderName = ProjectUtils.getFolderName(file.getContentType());

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", folderName)
            );

            return uploadResult.get("url").toString();

        } catch (IOException e) {
            throw new BadRequestException("Unable to upload file");
        }
    }
}
