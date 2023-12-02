package com.example.cloud_service.service;

import com.example.cloud_service.model.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FileService {

    @Value("${file.storage.path}")
    private String filePath;

    public void saveFile(MultipartFile file) {
        //сохранить файл на sd
        try (InputStream inputStream = file.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(filePath + "/" + file.getOriginalFilename())) {

            inputStream.transferTo(outputStream);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //сохранить запись в бд



    }
}