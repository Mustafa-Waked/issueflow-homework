package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.DependencyDtos;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketDependency;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Service
@Transactional
public class DependencyService {

    private final TicketDependencyRepository repository;
    private final TicketService ticketService;
    private final AuditService auditService;

    public DependencyService(TicketDependencyRepository repository, TicketService ticketService, AuditService auditService) {
        this.repository = repository;
        this.ticketService = ticketService;
        this.auditService = auditService;
    }

    public void add(Long ticketId, Long blockedBy, String actor) {
        if (ticketId.equals(blockedBy)) {
            throw new BadRequestException("Ticket cannot depend on itself");
        }

        Ticket ticket = ticketService.requireTicket(ticketId);
        Ticket blocker = ticketService.requireTicket(blockedBy);

        if (!ticket.getProject().getId().equals(blocker.getProject().getId())) {
            throw new BadRequestException("Tickets must belong to the same project");
        }
        if (repository.existsByTicketIdAndBlockedById(ticketId, blockedBy)) {
            throw new BadRequestException("Dependency already exists");
        }
        if (wouldCreateCycle(ticketId, blockedBy)) {
            throw new BadRequestException("Dependency cycle detected");
        }

        TicketDependency dependency = new TicketDependency();
        dependency.setTicket(ticket);
        dependency.setBlockedBy(blocker);
        repository.save(dependency);
        auditService.log(actor, "CREATE", "DEPENDENCY", ticketId + ":" + blockedBy, null, null);
    }

    public List<DependencyDtos.DependencyResponse> list(Long ticketId) {
        ticketService.requireTicket(ticketId);
        return repository.findByTicketId(ticketId).stream()
                .map(d -> new DependencyDtos.DependencyResponse(
                        d.getBlockedBy().getId(),
                        d.getBlockedBy().getTitle(),
                        d.getBlockedBy().getStatus()))
                .toList();
    }

    public void remove(Long ticketId, Long blockerId, String actor) {
        ticketService.requireTicket(ticketId);
        repository.findByTicketIdAndBlockedById(ticketId, blockerId)
                .ifPresentOrElse(repository::delete, () -> {
                    throw new NotFoundException("Dependency not found");
                });
        auditService.log(actor, "DELETE", "DEPENDENCY", ticketId + ":" + blockerId, null, null);
    }

    private boolean wouldCreateCycle(Long ticketId, Long blockedById) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(blockedById);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current.equals(ticketId)) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            for (TicketDependency dependency : repository.findByBlockedById(current)) {
                queue.add(dependency.getTicket().getId());
            }
        }
        return false;
    }
}
