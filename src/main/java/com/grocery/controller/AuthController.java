package com.grocery.controller;

import com.grocery.dto.ApiResponse;
import com.grocery.dto.AuthDto;
import com.grocery.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/login
     * Public - returns JWT token on successful login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.JwtResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.JwtResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /**
     * POST /api/auth/register
     * Public (or restrict to ADMIN in production)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}
