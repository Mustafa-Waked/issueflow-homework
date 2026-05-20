package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.ProjectDtos;
import com.att.tdp.issueflow.security.SecurityUtil;
import com.att.tdp.issueflow.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final SecurityUtil securityUtil;
    public ProjectController(ProjectService projectService, SecurityUtil securityUtil) { this.projectService = projectService; this.securityUtil = securityUtil; }

    @GetMapping public List<ProjectDtos.ProjectResponse> all() { return projectService.all(); }
    @GetMapping("/{projectId}") public ProjectDtos.ProjectResponse one(@PathVariable Long projectId) { return projectService.one(projectId); }
    @PostMapping public ProjectDtos.ProjectResponse create(@Valid @RequestBody ProjectDtos.CreateProjectRequest request) { return projectService.create(request, securityUtil.actor()); }
    @PatchMapping("/{projectId}") public void update(@PathVariable Long projectId, @RequestBody ProjectDtos.UpdateProjectRequest request) { projectService.update(projectId, request, securityUtil.actor()); }
    @DeleteMapping("/{projectId}") public void delete(@PathVariable Long projectId) { projectService.delete(projectId, securityUtil.actor()); }
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/deleted") public List<ProjectDtos.ProjectResponse> deleted() { return projectService.deleted(); }
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{projectId}/restore") public void restore(@PathVariable Long projectId) { projectService.restore(projectId, securityUtil.actor()); }
    @GetMapping("/{projectId}/workload") public List<ProjectDtos.WorkloadResponse> workload(@PathVariable Long projectId) { return projectService.workload(projectId); }
}
