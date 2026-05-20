package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.TicketDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {
    List<TicketDependency> findByTicketId(Long ticketId);
    List<TicketDependency> findByBlockedById(Long blockedById);
    Optional<TicketDependency> findByTicketIdAndBlockedById(Long ticketId, Long blockedById);
    boolean existsByTicketIdAndBlockedById(Long ticketId, Long blockedById);
    boolean existsByTicketIdAndBlockedByStatusNot(Long ticketId, com.att.tdp.issueflow.entity.TicketStatus status);
}
