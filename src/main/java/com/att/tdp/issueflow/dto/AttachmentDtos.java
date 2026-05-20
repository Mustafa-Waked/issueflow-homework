package com.att.tdp.issueflow.dto;

public class AttachmentDtos {
    public record AttachmentResponse(Long id, Long ticketId, String filename, String contentType) {}
}
