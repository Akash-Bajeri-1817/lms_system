package com.lms.user.controller;

import com.lms.common.ApiResponse;
import com.lms.user.dto.AuthResponse;
import com.lms.user.dto.LoginRequest;
import com.lms.user.dto.RegisterRequest;
import com.lms.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController                  // handles HTTP requests + auto-converts return value to JSON
@RequestMapping("/api/auth")     // all endpoints in this class start with /api/auth
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")

public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "User registered successfully")
        );
    }

    @Operation(summary = "Login and get JWT token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "User logged in successfully")
        );
    }
}
