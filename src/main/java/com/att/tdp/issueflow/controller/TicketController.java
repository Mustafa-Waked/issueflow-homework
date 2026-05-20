package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.TicketDtos;
import com.att.tdp.issueflow.security.SecurityUtil;
import com.att.tdp.issueflow.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private final TicketService ticketService;
    private final SecurityUtil securityUtil;

    public TicketController(TicketService ticketService, SecurityUtil securityUtil) {
        this.ticketService = ticketService;
        this.securityUtil = securityUtil;
    }

    @GetMapping
    public List<TicketDtos.TicketResponse> byProject(@RequestParam Long projectId) {
        return ticketService.byProject(projectId);
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TicketDtos.TicketResponse> deleted(@RequestParam Long projectId) {
        return ticketService.deleted(projectId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Long projectId) {
        byte[] data = ticketService.exportCsv(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(data);
    }

    @PostMapping("/import")
    public TicketDtos.ImportResult importCsv(@RequestParam("file") MultipartFile file,
                                             @RequestParam("projectId") Long projectId) {
        return ticketService.importCsv(file, projectId, securityUtil.actor());
    }

    @GetMapping("/{ticketId}")
    public TicketDtos.TicketResponse one(@PathVariable Long ticketId) {
        return ticketService.one(ticketId);
    }

    @PostMapping
    public TicketDtos.TicketResponse create(@Valid @RequestBody TicketDtos.CreateTicketRequest request) {
        return ticketService.create(request, securityUtil.actor());
    }

    @PatchMapping("/{ticketId}")
    public void update(@PathVariable Long ticketId, @RequestBody TicketDtos.UpdateTicketRequest request) {
        ticketService.update(ticketId, request, securityUtil.actor());
    }

    @DeleteMapping("/{ticketId}")
    public void delete(@PathVariable Long ticketId) {
        ticketService.delete(ticketId, securityUtil.actor());
    }

    @PostMapping("/{ticketId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public void restore(@PathVariable Long ticketId) {
        ticketService.restore(ticketId, securityUtil.actor());
    }
}
