package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.AuthLoginRequest;
import com.example.licenseplate.dto.request.AuthRegisterRequest;
import com.example.licenseplate.dto.request.UserProfileUpdateRequest;
import com.example.licenseplate.dto.response.AuthResponse;
import com.example.licenseplate.dto.response.AuthUserDto;
import com.example.licenseplate.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> getCurrentUser(
        @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.getCurrentUser(authHeader));
    }

    @PutMapping("/me")
    public ResponseEntity<AuthUserDto> updateCurrentUser(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(authService.updateCurrentUserProfile(authHeader, request));
    }
}
