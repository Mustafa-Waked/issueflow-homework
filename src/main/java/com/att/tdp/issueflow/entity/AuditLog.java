package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
        @Index(name = "idx_audit_actor", columnList = "actor"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();
}
