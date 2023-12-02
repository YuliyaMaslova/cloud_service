package com.example.cloud_service.service;

import com.example.cloud_service.entity.FileEntity;
import com.example.cloud_service.entity.UserEntity;
import com.example.cloud_service.exception.AccessDeniedException;
import com.example.cloud_service.model.FileDTO;
import com.example.cloud_service.model.EditFileRequest;
import com.example.cloud_service.repository.FileRepository;
import com.example.cloud_service.repository.UserRepository;
import com.example.cloud_service.security.ApplicationUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceImplTest {

    Path tmpdir = Files.createTempDirectory("user");



    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    private FileRepository fileRepository = Mockito.mock(FileRepository.class);

    private FileServiceImpl fileService = new FileServiceImpl(userRepository, fileRepository,
            tmpdir.toString());

    FileServiceImplTest() throws IOException {
    }

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        Path filePath = tmpdir.resolve("test.txt");
        Files.createFile(filePath);
        Files.write(filePath, "Test file content".getBytes());

    }


    @Test
    void saveFile_Success() throws IOException {
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());

        UserEntity userEntity = new UserEntity();
        userEntity.setId(1);

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(multipartFile.getSize()).thenReturn(100L);

        String fileName = "file2.txt";
        String filePath = "path/to/files";

        when(userRepository.findById(appUser.getId())).thenReturn(Optional.of(userEntity));

        fileService.saveFile("hash", multipartFile, fileName, appUser);

        verify(fileRepository, times(1)).save(Mockito.any(FileEntity.class));

        ArgumentCaptor<FileEntity> fileEntityCaptor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(fileEntityCaptor.capture());
        FileEntity savedFileEntity = fileEntityCaptor.getValue();

        assertEquals(fileName, savedFileEntity.getFileName());
        assertEquals(100L, savedFileEntity.getSize());
        assertEquals(userEntity, savedFileEntity.getUser());
        assertNotNull(savedFileEntity.getUploadedAt());
    }


    @Test
    void deleteFile_Success() throws IOException {

        String fileName = "test.txt";
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setSize(100L);
        fileEntity.setUploadedAt(LocalDateTime.now());
        fileEntity.setUser(userEntity);

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.of(fileEntity));

        fileService.deleteFile(fileName, appUser);

        verify(fileRepository).delete(fileEntity);
        verify(fileRepository).findByFileName(fileName);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void deleteFile_ThrowsFileNotFoundException() {
        int userId = 1;
        String fileName = "test.txt";
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.deleteFile(fileName, appUser));
        verify(fileRepository).findByFileName(fileName);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void deleteFile_ThrowsAccessDeniedException() {

        String fileName = "test.txt";
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(2); // Different user ID
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setSize(100L);
        fileEntity.setUploadedAt(LocalDateTime.now());
        fileEntity.setUser(userEntity);

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.of(fileEntity));

        assertThrows(AccessDeniedException.class, () -> fileService.deleteFile(fileName, appUser));
        verify(fileRepository).findByFileName(fileName);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void downloadFile_Success() throws IOException {

        String fileName = "test.txt";
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setSize(400L);

        fileEntity.setUploadedAt(LocalDateTime.now());
        fileEntity.setUser(userEntity);

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.of(fileEntity));

        byte[] fileData = fileService.downloadFile(fileName, appUser).getContentAsByteArray();

        assertNotNull(fileData);
        assertTrue(fileData.length > 0);
        verify(fileRepository).findByFileName(fileName);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void downloadFile_ThrowsFileNotFoundException() {
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());
        String fileName = "test.txt";


        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.downloadFile(fileName, appUser));
        verify(fileRepository).findByFileName(fileName);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void downloadFile_ThrowsAccessDeniedException() {

        String fileName = "test.txt";
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(2);
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setSize(100L);
        fileEntity.setUploadedAt(LocalDateTime.now());
        fileEntity.setUser(userEntity);

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.of(fileEntity));

        assertThrows(AccessDeniedException.class, () -> fileService.downloadFile(fileName, appUser));
        verify(fileRepository).findByFileName(fileName);
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void getAllFiles_Success() {
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());

        UserEntity userEntity = new UserEntity();
        userEntity.setId(1);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(1L);
        fileEntity.setSize(100L);
        fileEntity.setFileName("file1.txt");
        fileEntity.setUploadedAt(LocalDateTime.now());

        List<FileEntity> fileEntities = new ArrayList<>();
        fileEntities.add(fileEntity);

        Page<FileEntity> filePage = new PageImpl<>(fileEntities);


        when(userRepository.findById(appUser.getId())).thenReturn(Optional.of(userEntity));
        when(fileRepository.findByUser(userEntity, PageRequest.of(0, 10))).thenReturn(filePage);

        List<FileDTO> result = fileService.getAllFiles(10, appUser);

        assertEquals(1, result.size());

        FileDTO dto = result.get(0);
        assertEquals(fileEntity.getId(), dto.getId());
        assertEquals(fileEntity.getSize(), dto.getSize());
        assertEquals(fileEntity.getFileName(), dto.getFileName());
        assertEquals(fileEntity.getUploadedAt(), dto.getUploadedAt());

        verify(userRepository, times(1)).findById(appUser.getId());
        verify(fileRepository, times(1)).findByUser(userEntity, PageRequest.of(0, 10));
    }


    @Test
    void getAllFiles_UserNotFound() {

        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                fileService.getAllFiles(5, appUser));
    }


    @Test
    public void testEditFileName_Success() throws IOException {

        String fileName = "test.txt";
        String newFileName = "newFileName.txt";
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());
        UserEntity userEntity = new UserEntity();
        FileEntity fileEntity = new FileEntity();
        userEntity.setId(userId);
        fileEntity.setFileName(fileName);
        fileEntity.setUser(userEntity);

        EditFileRequest fileRequest = new EditFileRequest();
        fileRequest.setName(newFileName);

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.of(fileEntity));
        when(fileRepository.save(fileEntity)).thenReturn(fileEntity);

        EditFileRequest result = fileService.editFileName(fileName, fileRequest, appUser);

        assertEquals(newFileName, result.getName());
        verify(fileRepository, times(1)).findByFileName(fileName);
        verify(fileRepository, times(1)).save(fileEntity);
    }

    @Test
    public void testEditFileName_FileNotFound() {

        String fileName = "test.txt";
        EditFileRequest fileRequest = new EditFileRequest();
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () ->
                fileService.editFileName(fileName, fileRequest, appUser));

        verify(fileRepository, times(1)).findByFileName(fileName);
        verify(fileRepository, never()).save(any(FileEntity.class));
    }

    @Test
    public void testEditFileName_AccessDenied() {

        String fileName = "test.txt";
        EditFileRequest fileRequest = new EditFileRequest();
        fileRequest.setName(fileName);
        int userId = 1;
        ApplicationUser appUser = new ApplicationUser(userId, "username", "password", new ArrayList<>());

        UserEntity userEntity = new UserEntity();
        userEntity.setId(2);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setUser(userEntity);

        when(fileRepository.findByFileName(fileName)).thenReturn(Optional.of(fileEntity));

        assertThrows(AccessDeniedException.class, () ->
                fileService.editFileName(fileName, fileRequest, appUser));

        verify(fileRepository, times(1)).findByFileName(fileName);
        verify(fileRepository, never()).save(any(FileEntity.class));
    }

}


