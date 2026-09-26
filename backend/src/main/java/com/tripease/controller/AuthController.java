package com.tripease.controller;

import com.tripease.dto.Dtos.AuthResponse;
import com.tripease.dto.Dtos.LoginRequest;
import com.tripease.dto.Dtos.RegisterRequest;
import com.tripease.dto.Dtos.UserDto;
import com.tripease.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return auth.me(authentication.getName());
    }
}
