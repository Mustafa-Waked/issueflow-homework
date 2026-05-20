package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.repository.AttachmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock AttachmentRepository attachmentRepository;
    @Mock TicketService ticketService;
    @Mock AuditService auditService;

    @Test
    void acceptsAllowedMimeTypes() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        assertDoesNotThrow(() -> AttachmentService.validateFile(file));
    }

    @Test
    void rejectsInvalidMimeType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", new byte[]{1});
        assertThrows(BadRequestException.class, () -> AttachmentService.validateFile(file));
    }

    @Test
    void rejectsOversizedFile() {
        byte[] large = new byte[(int) AttachmentService.MAX_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", large);
        assertThrows(BadRequestException.class, () -> AttachmentService.validateFile(file));
    }
}
