package com.att.tdp.issueflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CommentDtos {
    public record CommentResponse(Long id, Long ticketId, Long authorId, String content, List<CommonDtos.IdNameDto> mentionedUsers, Long version) {}
    public record CreateCommentRequest(@NotNull Long authorId, @NotBlank String content) {}
    public record UpdateCommentRequest(@NotBlank String content, Long version) {}
}
