package com.tripease.service;

import com.tripease.dto.Dtos.AuthResponse;
import com.tripease.dto.Dtos.LoginRequest;
import com.tripease.dto.Dtos.RegisterRequest;
import com.tripease.dto.Dtos.UserDto;
import com.tripease.exception.ApiException;
import com.tripease.model.User;
import com.tripease.repository.UserRepository;
import com.tripease.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        users.save(user);
        return new AuthResponse(jwt.generate(email), toDto(user));
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmail(email)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return new AuthResponse(jwt.generate(email), toDto(user));
    }

    public UserDto me(String email) {
        return users.findByEmail(email).map(this::toDto)
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please sign in again."));
    }

    private UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail());
    }
}
