package com.example.cloud_service.service;

import com.example.cloud_service.model.FileDTO;
import com.example.cloud_service.model.EditFileRequest;
import com.example.cloud_service.security.ApplicationUser;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface FileService {
    public void saveFile(String hash, MultipartFile file, String fileName, ApplicationUser user);

    public void deleteFile(String fileName, ApplicationUser user) throws FileNotFoundException;

    public Resource downloadFile(String fileName, ApplicationUser user) throws FileNotFoundException, IOException;

    public EditFileRequest editFileName(String fileName, EditFileRequest fileRequest, ApplicationUser user ) throws IOException;

    public List<FileDTO> getAllFiles(Integer limit, ApplicationUser user);


}
