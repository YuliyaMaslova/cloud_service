package com.example.cloud_service.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String login;
    private String password;
}
