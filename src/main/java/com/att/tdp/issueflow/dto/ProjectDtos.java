package com.att.tdp.issueflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ProjectDtos {
    public record ProjectResponse(Long id, String name, String description, Long ownerId) {}
    public record CreateProjectRequest(@NotBlank String name, String description, @NotNull Long ownerId) {}
    public record UpdateProjectRequest(String name, String description) {}
    public record WorkloadResponse(Long userId, String username, long openTicketCount) {}
    public record ListResponse(List<ProjectResponse> data) {}
}
