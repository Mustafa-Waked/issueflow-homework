package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UserDtos {
    public record UserResponse(Long id, String username, String email, String fullName, UserRole role) {}
    public record CreateUserRequest(
            @NotBlank String username,
            @Email @NotBlank String email,
            @NotBlank String fullName,
            @NotNull UserRole role,
            @NotBlank String password
    ) {}
    public record UpdateUserRequest(String fullName, UserRole role) {}
    public record MentionPageResponse(List<CommentDtos.CommentResponse> data, long total, int page) {}
}
