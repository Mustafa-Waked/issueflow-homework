package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.AttachmentDtos;
import com.att.tdp.issueflow.entity.Attachment;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.repository.AttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@Transactional
public class AttachmentService {

    static final long MAX_BYTES = 10 * 1024 * 1024;
    static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain"
    );

    private final AttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final AuditService auditService;

    public AttachmentService(AttachmentRepository attachmentRepository, TicketService ticketService,
                               AuditService auditService) {
        this.attachmentRepository = attachmentRepository;
        this.ticketService = ticketService;
        this.auditService = auditService;
    }

    public AttachmentDtos.AttachmentResponse upload(Long ticketId, MultipartFile file, String actor) {
        validateFile(file);
        try {
            Attachment attachment = new Attachment();
            attachment.setTicket(ticketService.requireTicket(ticketId));
            attachment.setFilename(file.getOriginalFilename());
            attachment.setContentType(file.getContentType());
            attachment.setSizeBytes(file.getSize());
            attachment.setContent(file.getBytes());
            Attachment saved = attachmentRepository.save(attachment);
            auditService.log(actor, "CREATE", "ATTACHMENT", saved.getId().toString(), null, saved.getFilename());
            return new AttachmentDtos.AttachmentResponse(
                    saved.getId(), ticketId, saved.getFilename(), saved.getContentType());
        } catch (IOException e) {
            throw new BadRequestException("Attachment upload failed");
        }
    }

    public void delete(Long ticketId, Long attachmentId, String actor) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found"));
        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new NotFoundException("Attachment not found");
        }
        attachmentRepository.delete(attachment);
        auditService.log(actor, "DELETE", "ATTACHMENT", attachmentId.toString(), null, null);
    }

    static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Attachment exceeds maximum size of 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid attachment type. Allowed types: image/png, image/jpeg, application/pdf, text/plain");
        }
    }
}
