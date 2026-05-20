package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.dto.AuditDtos;
import com.att.tdp.issueflow.entity.AuditLog;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    public static final String SYSTEM_ACTOR = "SYSTEM";
    public static final String USER_ACTOR = "USER";

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;

    public AuditService(AuditLogRepository auditLogRepository, AppUserRepository appUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
    }

    public void log(String actor, String action, String entityType, String entityId, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setActor(normalizeStoredActor(actor));
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        auditLogRepository.save(log);
    }

    public List<AuditDtos.AuditResponse> get(String entityType, String entityId, String action, String actor,
                                             Integer page, Integer pageSize) {
        Specification<AuditLog> spec = buildSpec(entityType, entityId, action, actor);
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        if (page != null && pageSize != null) {
            Page<AuditLog> result = auditLogRepository.findAll(spec, PageRequest.of(Math.max(page - 1, 0), pageSize, sort));
            return result.getContent().stream().map(this::toResponse).toList();
        }
        return auditLogRepository.findAll(spec, sort).stream().map(this::toResponse).toList();
    }

    private Specification<AuditLog> buildSpec(String entityType, String entityId, String action, String actor) {
        Specification<AuditLog> spec = Specification.where(null);
        if (entityType != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("entityType"), entityType));
        }
        if (entityId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("entityId"), entityId));
        }
        if (action != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), action));
        }
        if (actor != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("actor"), actor));
        }
        return spec;
    }

    AuditDtos.AuditResponse toResponse(AuditLog log) {
        boolean system = SYSTEM_ACTOR.equalsIgnoreCase(log.getActor());
        String actorLabel = system ? SYSTEM_ACTOR : USER_ACTOR;
        Long performedBy = system
                ? null
                : appUserRepository.findByUsernameIgnoreCase(log.getActor()).map(u -> u.getId()).orElse(null);
        return new AuditDtos.AuditResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                performedBy,
                actorLabel,
                log.getOldValue(),
                log.getNewValue(),
                log.getTimestamp()
        );
    }

    private static String normalizeStoredActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return SYSTEM_ACTOR;
        }
        if (SYSTEM_ACTOR.equalsIgnoreCase(actor) || "system".equalsIgnoreCase(actor)) {
            return SYSTEM_ACTOR;
        }
        return actor;
    }
}
