package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.CommentDtos;
import com.att.tdp.issueflow.dto.UserDtos;
import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.Comment;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.mapper.IssueFlowMapper;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.MentionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock MentionRepository mentionRepository;
    @Mock TicketService ticketService;
    @Mock UserService userService;
    @Mock com.att.tdp.issueflow.repository.AppUserRepository appUserRepository;
    @Mock IssueFlowMapper mapper;
    @Mock AuditService auditService;

    CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(commentRepository, mentionRepository, ticketService, userService, appUserRepository, mapper, auditService);
    }

    @Test
    void parsesMentionsCaseInsensitivelyOnCreate() {
        Ticket ticket = new Ticket();
        ticket.setId(5L);
        AppUser author = user(2L, "author");
        AppUser mentioned = user(1L, "jdoe");

        when(ticketService.requireTicket(5L)).thenReturn(ticket);
        when(userService.requireUser(2L)).thenReturn(author);
        when(appUserRepository.findByUsernameIgnoreCase("JDOE")).thenReturn(java.util.Optional.of(mentioned));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(mentionRepository.findByCommentId(10L)).thenReturn(List.of());

        service.create(5L, new CommentDtos.CreateCommentRequest(2L, "Hello @JDOE"), "actor");

        verify(mentionRepository).save(any());
    }

    @Test
    void updateReevaluatesMentions() {
        Ticket ticket = new Ticket();
        ticket.setId(5L);
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setTicket(ticket);
        comment.setAuthor(user(2L, "author"));
        comment.setContent("old");

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(appUserRepository.findByUsernameIgnoreCase("asmith")).thenReturn(java.util.Optional.of(user(3L, "asmith")));

        service.update(5L, 10L, new CommentDtos.UpdateCommentRequest("Ping @asmith", null), "actor");

        verify(mentionRepository).deleteByCommentId(10L);
        verify(mentionRepository).save(any());
    }

    private AppUser user(Long id, String username) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setRole(UserRole.DEVELOPER);
        return user;
    }
}
