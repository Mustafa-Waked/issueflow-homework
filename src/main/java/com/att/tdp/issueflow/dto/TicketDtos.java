package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.entity.TicketPriority;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public class TicketDtos {
    public record TicketResponse(
            Long id,
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            TicketType type,
            Long projectId,
            Long assigneeId,
            LocalDateTime dueDate,
            boolean isOverdue,
            Long version
    ) {}

    public record CreateTicketRequest(
            @NotBlank String title,
            String description,
            @NotNull TicketStatus status,
            @NotNull TicketPriority priority,
            @NotNull TicketType type,
            @NotNull Long projectId,
            Long assigneeId,
            LocalDateTime dueDate
    ) {}
    public record UpdateTicketRequest(
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            Long assigneeId,
            LocalDateTime dueDate,
            Long version
    ) {}
    public record ImportResult(int created, int failed, List<String> errors) {}
    public record ImportRequest(MultipartFile file, Long projectId) {}
}
