package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.TicketDtos;
import com.att.tdp.issueflow.entity.*;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.mapper.IssueFlowMapper;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock ProjectService projectService;
    @Mock UserService userService;
    @Mock AppUserRepository appUserRepository;
    @Mock com.att.tdp.issueflow.repository.ProjectMemberRepository projectMemberRepository;
    @Mock ProjectMemberService projectMemberService;
    @Mock TicketDependencyRepository dependencyRepository;
    @Mock IssueFlowMapper mapper;
    @Mock AuditService auditService;

    TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, projectService, userService, appUserRepository,
                projectMemberRepository, projectMemberService, dependencyRepository, mapper, auditService);
    }

    @Test
    void rejectsDoneTicketUpdate() {
        Ticket ticket = activeTicket(TicketStatus.DONE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(BadRequestException.class, () ->
                service.update(1L, new TicketDtos.UpdateTicketRequest("x", null, null, null, null, null, null), "actor"));
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Ticket ticket = activeTicket(TicketStatus.TODO);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(BadRequestException.class, () ->
                service.update(1L, new TicketDtos.UpdateTicketRequest(null, null, TicketStatus.DONE, null, null, null, null), "actor"));
    }

    @Test
    void allowsValidStatusTransition() {
        Ticket ticket = activeTicket(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        service.update(1L, new TicketDtos.UpdateTicketRequest(null, null, TicketStatus.IN_REVIEW, null, null, null, null), "actor");

        assertEquals(TicketStatus.IN_REVIEW, ticket.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void rejectsDoneWhenBlockersOpen() {
        Ticket ticket = activeTicket(TicketStatus.IN_REVIEW);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(dependencyRepository.existsByTicketIdAndBlockedByStatusNot(1L, TicketStatus.DONE)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                service.update(1L, new TicketDtos.UpdateTicketRequest(null, null, TicketStatus.DONE, null, null, null, null), "actor"));
    }

    @Test
    void manualPriorityUpdateClearsOverdue() {
        Ticket ticket = activeTicket(TicketStatus.IN_PROGRESS);
        ticket.setOverdue(true);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        service.update(1L, new TicketDtos.UpdateTicketRequest(null, null, null, TicketPriority.LOW, null, null, null), "actor");

        assertFalse(ticket.isOverdue());
        assertEquals(TicketPriority.LOW, ticket.getPriority());
    }

    @Test
    void autoAssignsLeastLoadedDeveloperAndLogsSystemAudit() {
        Project project = new Project();
        project.setId(10L);
        when(projectService.requireProject(10L)).thenReturn(project);

        AppUser older = developer(1L, "dev1", Instant.parse("2020-01-01T00:00:00Z"));
        AppUser newer = developer(2L, "dev2", Instant.parse("2021-01-01T00:00:00Z"));
        when(projectMemberRepository.findDevelopersByProjectId(10L)).thenReturn(List.of(older, newer));
        when(ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(10L, 1L, TicketStatus.DONE)).thenReturn(2L);
        when(ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(10L, 2L, TicketStatus.DONE)).thenReturn(5L);

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        service.create(new TicketDtos.CreateTicketRequest(
                "t", "d", TicketStatus.TODO, TicketPriority.LOW, TicketType.BUG, 10L, null, null), "actor");

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, atLeastOnce()).log(eq(TicketService.SYSTEM_ACTOR), actionCaptor.capture(),
                eq("TICKET"), eq("99"), isNull(), anyString());
        assertTrue(actionCaptor.getAllValues().contains(TicketService.AUTO_ASSIGN_ACTION));
    }

    @Test
    void autoAssignmentExcludesAdminAndLeavesUnassignedWhenNoDevelopers() {
        Project project = new Project();
        project.setId(10L);
        when(projectService.requireProject(10L)).thenReturn(project);
        when(projectMemberRepository.findDevelopersByProjectId(10L)).thenReturn(List.of());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket saved = inv.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        service.create(new TicketDtos.CreateTicketRequest(
                "t", "d", TicketStatus.TODO, TicketPriority.LOW, TicketType.BUG, 10L, null, null), "actor");

        verify(ticketRepository).save(argThat(t -> t.getAssignee() == null));
        verify(auditService, never()).log(eq(TicketService.SYSTEM_ACTOR), eq(TicketService.AUTO_ASSIGN_ACTION),
                anyString(), anyString(), any(), any());
    }

    @Test
    void escalatesPriorityOneStepAndLogsSystemAudit() {
        Ticket ticket = activeTicket(TicketStatus.IN_PROGRESS);
        ticket.setDueDate(LocalDateTime.now().minusDays(1));
        ticket.setPriority(TicketPriority.LOW);
        when(ticketRepository.findByDeletedFalseAndDueDateBeforeAndStatusNot(any(), eq(TicketStatus.DONE)))
                .thenReturn(List.of(ticket));

        service.escalateOverdue();

        assertEquals(TicketPriority.MEDIUM, ticket.getPriority());
        verify(auditService).log(TicketService.SYSTEM_ACTOR, TicketService.AUTO_ESCALATION_ACTION,
                "TICKET", ticket.getId().toString(), TicketPriority.LOW.name(), TicketPriority.MEDIUM.name());
    }

    @Test
    void criticalOverdueSetsFlagOnlyOnce() {
        Ticket ticket = activeTicket(TicketStatus.IN_PROGRESS);
        ticket.setDueDate(LocalDateTime.now().minusDays(2));
        ticket.setPriority(TicketPriority.CRITICAL);
        ticket.setOverdue(false);
        when(ticketRepository.findByDeletedFalseAndDueDateBeforeAndStatusNot(any(), eq(TicketStatus.DONE)))
                .thenReturn(List.of(ticket));

        service.escalateOverdue();
        service.escalateOverdue();

        assertTrue(ticket.isOverdue());
        verify(auditService, times(1)).log(TicketService.SYSTEM_ACTOR, TicketService.AUTO_ESCALATION_ACTION,
                "TICKET", ticket.getId().toString(), "false", "true");
    }

    private Ticket activeTicket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(status);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setType(TicketType.BUG);
        ticket.setDeleted(false);
        Project project = new Project();
        project.setId(10L);
        ticket.setProject(project);
        return ticket;
    }

    private AppUser developer(Long id, String username, Instant createdAt) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(UserRole.DEVELOPER);
        user.setCreatedAt(createdAt);
        return user;
    }
}
