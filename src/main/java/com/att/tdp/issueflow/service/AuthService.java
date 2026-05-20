package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.AuthDtos;
import com.att.tdp.issueflow.entity.RevokedToken;
import com.att.tdp.issueflow.exception.UnauthorizedException;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.RevokedTokenRepository;
import com.att.tdp.issueflow.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RevokedTokenRepository revokedTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        var user = userRepository.findByUsernameIgnoreCase(req.username()).orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthDtos.LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }

    public void logout(String bearer) {
        if (bearer == null || !bearer.startsWith("Bearer ")) return;
        String token = bearer.substring(7);
        Claims claims = jwtService.parseClaims(token);
        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setToken(token);
        revokedToken.setExpiresAt(claims.getExpiration().toInstant());
        revokedTokenRepository.save(revokedToken);
    }

    public AuthDtos.MeResponse me(String username) {
        var user = userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> new UnauthorizedException("Unknown user"));
        return new AuthDtos.MeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
