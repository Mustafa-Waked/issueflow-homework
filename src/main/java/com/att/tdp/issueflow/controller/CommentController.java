package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.CommentDtos;
import com.att.tdp.issueflow.security.SecurityUtil;
import com.att.tdp.issueflow.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
public class CommentController {
    private final CommentService commentService;
    private final SecurityUtil securityUtil;
    public CommentController(CommentService commentService, SecurityUtil securityUtil) { this.commentService = commentService; this.securityUtil = securityUtil; }

    @GetMapping public List<CommentDtos.CommentResponse> list(@PathVariable Long ticketId) { return commentService.list(ticketId); }
    @PostMapping public CommentDtos.CommentResponse create(@PathVariable Long ticketId, @Valid @RequestBody CommentDtos.CreateCommentRequest request) { return commentService.create(ticketId, request, securityUtil.actor()); }
    @PatchMapping("/{commentId}") public void update(@PathVariable Long ticketId, @PathVariable Long commentId, @Valid @RequestBody CommentDtos.UpdateCommentRequest request) { commentService.update(ticketId, commentId, request, securityUtil.actor()); }
    @DeleteMapping("/{commentId}") public void delete(@PathVariable Long ticketId, @PathVariable Long commentId) { commentService.delete(ticketId, commentId, securityUtil.actor()); }
}
