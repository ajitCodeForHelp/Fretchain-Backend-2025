package com.bt.fretchain.primary.controller;

import com.bt.fretchain.bean.ResponseData;
import com.bt.fretchain.primary.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/v1/file")
@RequiredArgsConstructor
public class FileController extends _BaseController {

    private final FileUploadService fileUploadService;


    @PostMapping("/upload")
    public ResponseEntity<ResponseData> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return new ResponseEntity<>(ResponseData.builder()
                .statusCode(200)
                .message("file_uploaded_successfully")
                .data(fileUploadService.uploadCloudinaryFile(file))
//                .responsePacket(s3Service.uploadFile(file))
                .build(), HttpStatus.OK);

    }
}

