package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.entity.UserRole;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
    public record MeResponse(Long id, String username, String email, String fullName, UserRole role) {}
}
