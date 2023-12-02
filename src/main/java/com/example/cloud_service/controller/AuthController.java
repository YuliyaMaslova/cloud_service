package com.example.cloud_service.controller;

import com.example.cloud_service.model.AuthDTO;
import com.example.cloud_service.model.LoginResponse;
import com.example.cloud_service.security.ApplicationUser;
import com.example.cloud_service.security.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthController {
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody AuthDTO authDTO) {
        var authentication = new UsernamePasswordAuthenticationToken(authDTO.getLogin(), authDTO.getPassword());
        ApplicationUser applicationUser = (ApplicationUser) authenticationManager.authenticate(authentication).getPrincipal();
        String authToken = tokenService.issueToken(applicationUser);
        return new LoginResponse(authToken);
    }

    @PostMapping("/logout")
    public void logout(Authentication authentication) {
        tokenService.revokeToken(((String) authentication.getCredentials()));
    }
}
