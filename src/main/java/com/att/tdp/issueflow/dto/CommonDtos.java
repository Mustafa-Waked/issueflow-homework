package com.att.tdp.issueflow.dto;

import java.time.Instant;
import java.util.Map;

public class CommonDtos {
    public record ApiError(
            String code,
            String message,
            Map<String, String> validationErrors,
            Instant timestamp
    ) {}

    public record IdNameDto(Long id, String username, String fullName) {}
}
