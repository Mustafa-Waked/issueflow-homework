package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.CommentDtos;
import com.att.tdp.issueflow.dto.CommonDtos;
import com.att.tdp.issueflow.entity.Comment;
import com.att.tdp.issueflow.entity.Mention;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.mapper.IssueFlowMapper;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.MentionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentService {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]+)");
    private final CommentRepository commentRepository;
    private final MentionRepository mentionRepository;
    private final TicketService ticketService;
    private final UserService userService;
    private final AppUserRepository appUserRepository;
    private final IssueFlowMapper mapper;
    private final AuditService auditService;

    public CommentService(CommentRepository commentRepository, MentionRepository mentionRepository, TicketService ticketService,
                          UserService userService, AppUserRepository appUserRepository, IssueFlowMapper mapper, AuditService auditService) {
        this.commentRepository = commentRepository;
        this.mentionRepository = mentionRepository;
        this.ticketService = ticketService;
        this.userService = userService;
        this.appUserRepository = appUserRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<CommentDtos.CommentResponse> list(Long ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(c -> mapper.toComment(c, mentionsFor(c))).toList();
    }

    public CommentDtos.CommentResponse create(Long ticketId, CommentDtos.CreateCommentRequest req, String actor) {
        Comment c = new Comment();
        c.setTicket(ticketService.requireTicket(ticketId));
        c.setAuthor(userService.requireUser(req.authorId()));
        c.setContent(req.content());
        Comment saved = commentRepository.save(c);
        saveMentions(saved);
        auditService.log(actor, "CREATE", "COMMENT", saved.getId().toString(), null, req.content());
        return mapper.toComment(saved, mentionsFor(saved));
    }

    public void update(Long ticketId, Long commentId, CommentDtos.UpdateCommentRequest req, String actor) {
        Comment c = require(commentId);
        if (!Objects.equals(c.getTicket().getId(), ticketId)) throw new NotFoundException("Comment not found");
        if (req.version() != null && !req.version().equals(c.getVersion())) {
            throw new ConflictException("Comment was modified by another user");
        }
        c.setContent(req.content());
        commentRepository.save(c);
        mentionRepository.deleteByCommentId(c.getId());
        saveMentions(c);
        auditService.log(actor, "UPDATE", "COMMENT", c.getId().toString(), null, req.content());
    }

    public void delete(Long ticketId, Long commentId, String actor) {
        Comment c = require(commentId);
        if (!Objects.equals(c.getTicket().getId(), ticketId)) throw new NotFoundException("Comment not found");
        commentRepository.delete(c);
        auditService.log(actor, "DELETE", "COMMENT", c.getId().toString(), null, null);
    }

    private void saveMentions(Comment comment) {
        Set<String> usernames = new HashSet<>();
        Matcher m = MENTION_PATTERN.matcher(comment.getContent());
        while (m.find()) usernames.add(m.group(1));
        for (String username : usernames) {
            appUserRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
                Mention mention = new Mention();
                mention.setComment(comment);
                mention.setUser(user);
                mentionRepository.save(mention);
            });
        }
    }

    private List<CommonDtos.IdNameDto> mentionsFor(Comment comment) {
        return mentionRepository.findByCommentId(comment.getId()).stream()
                .map(m -> new CommonDtos.IdNameDto(m.getUser().getId(), m.getUser().getUsername(), m.getUser().getFullName()))
                .toList();
    }

    private Comment require(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new NotFoundException("Comment not found"));
    }
}
