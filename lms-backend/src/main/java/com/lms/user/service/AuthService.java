package com.lms.user.service;

import com.lms.user.dto.AuthResponse;
import com.lms.user.dto.LoginRequest;
import com.lms.user.dto.RegisterRequest;
import com.lms.user.entity.Role;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {

        // 1. check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // 2. build the User entity
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // hash the password
                .role(Role.STUDENT)       // default role for new registrations
                .enabled(true)
                .build();

        // 3. save to database
        userRepository.save(user);

        // 4. generate JWT token
        var token = jwtService.generateToken(user);

        // 5. return token + user info
        return AuthResponse.builder()
                .accessToken(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        // 1. this line does two things:
        //    - loads user from DB by email
        //    - verifies the password matches the BCrypt hash
        //    - throws BadCredentialsException automatically if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. if we reach here, credentials are valid — load the user
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. generate token and return
        var token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}