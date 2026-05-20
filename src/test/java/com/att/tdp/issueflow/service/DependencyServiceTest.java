package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketDependency;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DependencyServiceTest {

    @Mock TicketDependencyRepository repository;
    @Mock TicketService ticketService;
    @Mock AuditService auditService;

    DependencyService service;

    @BeforeEach
    void setUp() {
        service = new DependencyService(repository, ticketService, auditService);
    }

    @Test
    void rejectsSelfDependency() {
        assertThrows(BadRequestException.class, () -> service.add(1L, 1L, "actor"));
    }

    @Test
    void rejectsDifferentProjects() {
        Ticket ticket = ticket(1L, 10L);
        Ticket blocker = ticket(2L, 20L);
        when(ticketService.requireTicket(1L)).thenReturn(ticket);
        when(ticketService.requireTicket(2L)).thenReturn(blocker);

        assertThrows(BadRequestException.class, () -> service.add(1L, 2L, "actor"));
    }

    @Test
    void rejectsDuplicateDependency() {
        Ticket ticket = ticket(1L, 10L);
        Ticket blocker = ticket(2L, 10L);
        when(ticketService.requireTicket(1L)).thenReturn(ticket);
        when(ticketService.requireTicket(2L)).thenReturn(blocker);
        when(repository.existsByTicketIdAndBlockedById(1L, 2L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.add(1L, 2L, "actor"));
    }

    @Test
    void rejectsDependencyCycle() {
        Ticket ticket = ticket(1L, 10L);
        Ticket blocker = ticket(2L, 10L);
        when(ticketService.requireTicket(1L)).thenReturn(ticket);
        when(ticketService.requireTicket(2L)).thenReturn(blocker);
        when(repository.existsByTicketIdAndBlockedById(1L, 2L)).thenReturn(false);

        TicketDependency existing = new TicketDependency();
        existing.setTicket(ticket(1L, 10L));
        existing.setBlockedBy(blocker);
        when(repository.findByBlockedById(2L)).thenReturn(List.of(existing));

        assertThrows(BadRequestException.class, () -> service.add(1L, 2L, "actor"));
    }

    private Ticket ticket(Long id, Long projectId) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setStatus(TicketStatus.TODO);
        Project project = new Project();
        project.setId(projectId);
        ticket.setProject(project);
        return ticket;
    }
}
