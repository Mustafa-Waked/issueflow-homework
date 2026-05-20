package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.mapper.IssueFlowMapper;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock UserService userService;
    @Mock AppUserRepository appUserRepository;
    @Mock com.att.tdp.issueflow.repository.ProjectMemberRepository projectMemberRepository;
    @Mock ProjectMemberService projectMemberService;
    @Mock TicketRepository ticketRepository;
    @Mock IssueFlowMapper mapper;
    @Mock AuditService auditService;

    ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, userService, appUserRepository, projectMemberRepository,
                projectMemberService, ticketRepository, mapper, auditService);
    }

    @Test
    void hidesSoftDeletedProject() {
        Project deleted = new Project();
        deleted.setId(1L);
        deleted.setDeleted(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertThrows(NotFoundException.class, () -> service.requireProject(1L));
    }

    @Test
    void workloadSortedAscendingAndIncludesDevelopersOnly() {
        Project project = new Project();
        project.setId(7L);
        project.setDeleted(false);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        AppUser dev1 = new AppUser();
        dev1.setId(1L);
        dev1.setUsername("a");
        dev1.setRole(UserRole.DEVELOPER);
        AppUser dev2 = new AppUser();
        dev2.setId(2L);
        dev2.setUsername("b");
        dev2.setRole(UserRole.DEVELOPER);

        when(projectMemberRepository.findDevelopersByProjectId(7L)).thenReturn(List.of(dev1, dev2));
        when(ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(7L, 1L, TicketStatus.DONE)).thenReturn(3L);
        when(ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(7L, 2L, TicketStatus.DONE)).thenReturn(1L);

        var workload = service.workload(7L);

        assertEquals(2, workload.size());
        assertEquals(1L, workload.get(0).openTicketCount());
        assertEquals(3L, workload.get(1).openTicketCount());
    }
}
