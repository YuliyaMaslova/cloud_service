package com.example.cloud_service.controller;

import com.example.cloud_service.entity.UserEntity;
import com.example.cloud_service.model.AuthDTO;
import com.example.cloud_service.model.LoginResponse;
import com.example.cloud_service.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTestContainers {

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

    @Test
    public void unauthorizedLoginTest() throws Exception {
        // given
        AuthDTO authRequest = new AuthDTO();
        authRequest.setLogin("123");
        authRequest.setPassword("321");

        // when
        ResultActions result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(authRequest)));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", Matchers.is("Bad credentials")));
    }

    @Test
    public void successLoginTest() throws Exception {
        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setLogin("123");
        userEntity.setPassword("321");

        userRepository.save(userEntity);

        AuthDTO authRequest = new AuthDTO();
        authRequest.setLogin(userEntity.getLogin());
        authRequest.setPassword(userEntity.getPassword());

        // when
        ResultActions result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(authRequest)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.authToken", Matchers.notNullValue()));
    }

    @Test
    public void successLogoutTest() throws Exception {
        //given
        UserEntity userEntity = new UserEntity();
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

        //получаю токен
        String authToken = loginResponse.getAuthToken();

        //when
        ResultActions result = mockMvc.perform(post("/logout")
                .header("auth-token", authToken)
                .contentType(MediaType.APPLICATION_JSON));

        //then
        result.andExpect(status().isOk());
    }
}