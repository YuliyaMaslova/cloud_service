package com.example.cloud_service.service;

import com.example.cloud_service.entity.FileEntity;
import com.example.cloud_service.exception.AccessDeniedException;
import com.example.cloud_service.model.FileDTO;
import com.example.cloud_service.model.EditFileRequest;
import com.example.cloud_service.repository.FileRepository;
import com.example.cloud_service.repository.UserRepository;
import com.example.cloud_service.security.ApplicationUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final String filePath;

    public FileServiceImpl(UserRepository userRepository,
                           FileRepository fileRepository,
                           @Value("${file.storage.path}") String filePath) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.filePath = filePath;
    }


    public void saveFile(String hash, MultipartFile file, String fileName, ApplicationUser user) {

        try (InputStream inputStream = file.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(filePath + "/" + fileName)) {

            inputStream.transferTo(outputStream);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден. id: " + user.getId()));

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setSize(file.getSize());
        fileEntity.setUploadedAt(LocalDateTime.now());
        fileEntity.setUser(foundUser);

        fileRepository.save(fileEntity);

    }

    @Override
    public void deleteFile(String fileName, ApplicationUser user) throws FileNotFoundException {
        FileEntity fileEntity = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new FileNotFoundException("Файл с таким именем не найден: " + fileName));
        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Доступ запрещен.У пользователя нет разрешения на удаление файла.");
        }
        try {
            Files.delete(Path.of(filePath, fileName));
            fileRepository.delete(fileEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Resource downloadFile(String fileName, ApplicationUser user) throws IOException {
        FileEntity fileEntity = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new FileNotFoundException("Файл с таким именем не найден: " + fileName));
        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Доступ запрещен.У пользователя нет разрешения на скачивание файла.");
        }

        return new FileSystemResource(Paths.get(filePath, fileName));
    }

    @Override
    public EditFileRequest editFileName(String fileName, EditFileRequest fileRequest, ApplicationUser user) throws IOException {
        FileEntity fileEntity = fileRepository.findByFileName(fileName)
                .orElseThrow(() -> new FileNotFoundException("Файл с таким именем не найден: " + fileName));
        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Доступ запрещен.У пользователя нет разрешения на изменение файла.");
        }

        File fileToMove = new File(filePath, fileName);
        boolean isMoved = fileToMove.renameTo(new File(filePath, fileRequest.getName()));
        fileEntity.setFileName(fileRequest.getName());
        fileRepository.save(fileEntity);
        if (!isMoved) {
            throw new FileSystemException(filePath);
        }
        return fileRequest;
    }

    @Override
    public List<FileDTO> getAllFiles(Integer limit, ApplicationUser user) {
        var foundUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден. id: " + user.getId()));

        Page<FileEntity> files = fileRepository.findByUser(foundUser, PageRequest.of(0, limit));

        return files.getContent().stream()
                .map(fileEntity -> {
                    FileDTO dto = new FileDTO();
                    dto.setId(fileEntity.getId());
                    dto.setSize(fileEntity.getSize());
                    dto.setFileName(fileEntity.getFileName());
                    dto.setUploadedAt(fileEntity.getUploadedAt());
                    return dto;
                })
                .toList();
    }


}













