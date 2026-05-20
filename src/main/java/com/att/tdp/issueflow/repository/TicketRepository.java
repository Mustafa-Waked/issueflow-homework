package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(Long projectId);
    List<Ticket> findByProjectIdAndDeletedTrueOrderByCreatedAtDesc(Long projectId);
    long countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(Long projectId, Long assigneeId, TicketStatus status);

    @Query("""
            select t from Ticket t
            where t.project.id = :projectId and t.deleted = false
            and t.status <> com.att.tdp.issueflow.entity.TicketStatus.DONE
            """)
    List<Ticket> findOpenByProjectId(Long projectId);

    List<Ticket> findByDeletedFalseAndDueDateBeforeAndStatusNot(java.time.LocalDateTime now, TicketStatus status);
}
