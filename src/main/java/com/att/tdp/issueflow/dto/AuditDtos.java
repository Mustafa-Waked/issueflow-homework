package com.att.tdp.issueflow.dto;

import java.time.Instant;

public class AuditDtos {
    /**
     * Matches README contract: performedBy (user id), actor (USER|SYSTEM), plus old/new values from DB.
     */
    public record AuditResponse(
            Long id,
            String action,
            String entityType,
            String entityId,
            Long performedBy,
            String actor,
            String oldValue,
            String newValue,
            Instant timestamp
    ) {}
}
