package com.example.cloud_service.security;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TokenService {
    private final ConcurrentMap<String, ApplicationUser> tokens = new ConcurrentHashMap<>();

    public String issueToken(ApplicationUser applicationUser) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, applicationUser);
        return token;
    }

    public ApplicationUser validateToken(String authToken) {
        return authToken != null ? tokens.get(authToken) : null;
    }

    public void revokeToken(String token) {
        tokens.remove(token);
    }
}
