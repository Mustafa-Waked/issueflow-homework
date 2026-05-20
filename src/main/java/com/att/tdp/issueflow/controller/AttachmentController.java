package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.AttachmentDtos;
import com.att.tdp.issueflow.security.SecurityUtil;
import com.att.tdp.issueflow.service.AttachmentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;
    private final SecurityUtil securityUtil;
    public AttachmentController(AttachmentService attachmentService, SecurityUtil securityUtil) { this.attachmentService = attachmentService; this.securityUtil = securityUtil; }

    @PostMapping public AttachmentDtos.AttachmentResponse upload(@PathVariable Long ticketId, @RequestParam("file") MultipartFile file) { return attachmentService.upload(ticketId, file, securityUtil.actor()); }
    @DeleteMapping("/{attachmentId}") public void delete(@PathVariable Long ticketId, @PathVariable Long attachmentId) { attachmentService.delete(ticketId, attachmentId, securityUtil.actor()); }
}
