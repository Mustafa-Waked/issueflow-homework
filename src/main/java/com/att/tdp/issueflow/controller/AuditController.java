package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.AuditDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit-logs")
public class AuditController {
    private final AuditService auditService;
    public AuditController(AuditService auditService) { this.auditService = auditService; }

    /**
     * Returns audit entries newest first. Optional {@code page} and {@code pageSize} enable pagination;
     * if omitted, all matching rows are returned (may be large on busy systems).
     */
    @GetMapping
    public List<AuditDtos.AuditResponse> get(@RequestParam(required = false) String entityType,
                                             @RequestParam(required = false) String entityId,
                                             @RequestParam(required = false) String action,
                                             @RequestParam(required = false) String actor,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer pageSize) {
        return auditService.get(entityType, entityId, action, actor, page, pageSize);
    }
}
