package com.example.cloud_service;

import com.example.cloud_service.entity.FileEntity;
import com.example.cloud_service.entity.UserEntity;
import com.example.cloud_service.model.AuthDTO;
import com.example.cloud_service.model.EditFileRequest;
import com.example.cloud_service.model.LoginResponse;
import com.example.cloud_service.repository.FileRepository;
import com.example.cloud_service.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FileIntegrationTestsContainers {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("postgres")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    private static void propertiesTest(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url=", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username=", POSTGRES::getUsername);
        registry.add("spring.datasource.password=", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    private UserEntity userEntity;
    private String authToken;

    @Value("${file.storage.path}")
    private String filePath;

    @BeforeEach
    public void setup() throws Exception {
        userEntity = new UserEntity();
        userEntity.setLogin("123");
        userEntity.setPassword("456");

        userRepository.save(userEntity);

        AuthDTO authRequest = new AuthDTO();
        authRequest.setLogin(userEntity.getLogin());
        authRequest.setPassword(userEntity.getPassword());

        ResultActions loginResult = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(authRequest)));

        byte[] responseBody = loginResult.andReturn().getResponse().getContentAsByteArray();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);

        authToken = loginResponse.getAuthToken();
    }

    @Test
    public void uploadFile() throws Exception {
        String filename = "filename.txt";
        MockMultipartFile file = new MockMultipartFile("file", filename,
                "text/plain", "Test file contents".getBytes());

        mockMvc.perform(multipart("/file/upload")
                .file(file)
                .header("auth-token", authToken)
                .queryParam("hash", "hashValue")
                .queryParam("filename", filename)
        ).andExpect(status().isOk());

        Optional<FileEntity> savedFile = fileRepository.findByFileName(filename);

        assertTrue(savedFile.isPresent());
        File diskFile = new File(filePath, filename);
        Path filePath = Paths.get(this.filePath, filename);
        assertTrue(diskFile.exists());
        assertEquals(diskFile.getAbsolutePath(), filePath.toString());

    }

    @Test
    public void deleteFile() throws Exception {
        String filename = "filename.txt";
        MockMultipartFile file = new MockMultipartFile("file", filename,
                "text/plain", "Test file contents".getBytes());

        mockMvc.perform(multipart("/file/upload")
                .file(file)
                .header("auth-token", authToken)
                .queryParam("hash", "hashValue")
                .queryParam("filename", filename)
        ).andExpect(status().isOk());


        mockMvc.perform(delete("/file/delete", filename)
                .header("auth-token", authToken)
                .queryParam("filename", filename)
        ).andExpect(status().isOk());

    }

    @Test
    public void downloadFile() throws Exception {
        String filename = "filename.txt";
        String fileContent = "Test file contents";
        MockMultipartFile file = new MockMultipartFile("file", filename,
                "text/plain", fileContent.getBytes());

        mockMvc.perform(multipart("/file/upload")
                .file(file)
                .header("auth-token", authToken)
                .queryParam("hash", "hashValue")
                .queryParam("filename", filename)
        ).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/file/download")
                        .header("auth-token", authToken)
                        .queryParam("filename", filename)
                ).andExpect(status().isOk())
                .andReturn();

        assertEquals(fileContent, result.getResponse().getContentAsString());
    }

    @Test
    public void editFile() throws Exception {
        String filename = "filename.txt";
        String fileContent = "Test file contents";
        MockMultipartFile file = new MockMultipartFile("file", filename,
                "text/plain", fileContent.getBytes());

        EditFileRequest fileRequest = new EditFileRequest();
        fileRequest.setName("newFilename.txt");

        mockMvc.perform(multipart("/file/upload")
                .file(file)
                .header("auth-token", authToken)
                .queryParam("hash", "hashValue")
                .queryParam("filename", filename)
        ).andExpect(status().isOk());


        mockMvc.perform(put("/file/edit")
                        .header("auth-token", authToken)
                        .queryParam("filename", filename)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fileRequest))
        ).andExpect(status().isOk());

        Optional<FileEntity> savedFile = fileRepository.findByFileName(fileRequest.getName());

        assertTrue(savedFile.isPresent());
        assertEquals(fileRequest.getName(), savedFile.get().getFileName());

    }

    @Test
    public void getAllFiles() throws Exception {
        List<FileEntity> files = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            FileEntity fileEntity = new FileEntity();
            String fileName = "file_" + (i + 1) + ".txt";
            fileEntity.setFileName(fileName);

            long fileSize = new Random().nextInt(5) + 5;
            fileEntity.setSize(fileSize);

            fileEntity.setUploadedAt(LocalDateTime.now());

            fileEntity.setUser(userEntity);
            files.add(fileEntity);
        }

        fileRepository.saveAll(files);


        MvcResult result = mockMvc.perform(get("/file/list")
                        .header("auth-token", authToken)
                        .queryParam("limit", "3")
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andReturn();
    }

}
