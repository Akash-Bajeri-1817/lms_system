package com.lms.user.controller;

import com.lms.user.dto.AuthResponse;
import com.lms.user.dto.LoginRequest;
import com.lms.user.dto.RegisterRequest;
import com.lms.user.service.AuthService;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request
            // @RequestBody  = read JSON from request body and convert to RegisterRequest object
            // @Valid        = run the validation annotations (@NotBlank, @Email etc.)
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}