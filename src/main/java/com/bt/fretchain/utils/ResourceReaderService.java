package com.bt.fretchain.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ResourceReaderService {

    public FileInputStream getResourceAsFileInputStream(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        File file = new File(resource.getPath());
        return new FileInputStream(file);
    }

    public InputStream getResourceAsInputStream(String resourcePath) throws IOException {
        return new ClassPathResource(resourcePath).getInputStream();
    }
}
