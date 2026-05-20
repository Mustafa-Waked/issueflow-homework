package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ticket_dependency", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_blocker", columnNames = {"ticket_id", "blocked_by_id"})
})
@Getter
@Setter
public class TicketDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_by_id")
    private Ticket blockedBy;
}
