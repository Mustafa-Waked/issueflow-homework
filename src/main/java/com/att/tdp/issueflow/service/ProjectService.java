package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.ProjectDtos;
import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.mapper.IssueFlowMapper;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.ProjectMemberRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final AppUserRepository appUserRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberService projectMemberService;
    private final TicketRepository ticketRepository;
    private final IssueFlowMapper mapper;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository, UserService userService, AppUserRepository appUserRepository,
                          ProjectMemberRepository projectMemberRepository, ProjectMemberService projectMemberService,
                          TicketRepository ticketRepository, IssueFlowMapper mapper, AuditService auditService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.appUserRepository = appUserRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectMemberService = projectMemberService;
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<ProjectDtos.ProjectResponse> all() { return projectRepository.findByDeletedFalseOrderByCreatedAtDesc().stream().map(mapper::toProject).toList(); }
    public ProjectDtos.ProjectResponse one(Long id) { return mapper.toProject(requireProject(id)); }
    public List<ProjectDtos.ProjectResponse> deleted() { return projectRepository.findByDeletedTrueOrderByCreatedAtDesc().stream().map(mapper::toProject).toList(); }

    public ProjectDtos.ProjectResponse create(ProjectDtos.CreateProjectRequest req, String actor) {
        Project p = new Project();
        p.setName(req.name());
        p.setDescription(req.description());
        p.setOwner(userService.requireUser(req.ownerId()));
        Project saved = projectRepository.save(p);
        projectMemberService.linkDeveloper(saved, saved.getOwner());
        auditService.log(actor, "CREATE", "PROJECT", saved.getId().toString(), null, req.toString());
        return mapper.toProject(saved);
    }

    public void update(Long id, ProjectDtos.UpdateProjectRequest req, String actor) {
        Project p = requireProject(id);
        if (req.name() != null) p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        projectRepository.save(p);
        auditService.log(actor, "UPDATE", "PROJECT", id.toString(), null, req.toString());
    }
    public void delete(Long id, String actor) { Project p = requireProject(id); p.setDeleted(true); projectRepository.save(p); auditService.log(actor, "SOFT_DELETE", "PROJECT", id.toString(), null, null); }
    public void restore(Long id, String actor) { Project p = requireAnyProject(id); p.setDeleted(false); projectRepository.save(p); auditService.log(actor, "RESTORE", "PROJECT", id.toString(), null, null); }

    public List<ProjectDtos.WorkloadResponse> workload(Long projectId) {
        requireProject(projectId);
        return projectMemberRepository.findDevelopersByProjectId(projectId).stream()
                .map(dev -> new ProjectDtos.WorkloadResponse(
                        dev.getId(),
                        dev.getUsername(),
                        ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(
                                projectId, dev.getId(), TicketStatus.DONE)))
                .sorted(Comparator.comparingLong(ProjectDtos.WorkloadResponse::openTicketCount))
                .toList();
    }

    public Project requireProject(Long id) {
        Project p = requireAnyProject(id);
        if (p.isDeleted()) throw new NotFoundException("Project not found");
        return p;
    }
    public Project requireAnyProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
    }
}
