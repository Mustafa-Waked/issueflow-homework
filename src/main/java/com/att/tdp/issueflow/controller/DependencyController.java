package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.DependencyDtos;
import com.att.tdp.issueflow.security.SecurityUtil;
import com.att.tdp.issueflow.service.DependencyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
public class DependencyController {
    private final DependencyService dependencyService;
    private final SecurityUtil securityUtil;
    public DependencyController(DependencyService dependencyService, SecurityUtil securityUtil) { this.dependencyService = dependencyService; this.securityUtil = securityUtil; }

    @PostMapping public void add(@PathVariable Long ticketId, @Valid @RequestBody DependencyDtos.AddDependencyRequest request) { dependencyService.add(ticketId, request.blockedBy(), securityUtil.actor()); }
    @GetMapping public List<DependencyDtos.DependencyResponse> list(@PathVariable Long ticketId) { return dependencyService.list(ticketId); }
    @DeleteMapping("/{blockerId}") public void remove(@PathVariable Long ticketId, @PathVariable Long blockerId) { dependencyService.remove(ticketId, blockerId, securityUtil.actor()); }
}
