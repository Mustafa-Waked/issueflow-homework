package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class DependencyDtos {
    public record AddDependencyRequest(@NotNull Long blockedBy) {}
    public record DependencyResponse(Long id, String title, TicketStatus status) {}
}
