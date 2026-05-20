package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.CommentDtos;
import com.att.tdp.issueflow.dto.CommonDtos;
import com.att.tdp.issueflow.dto.UserDtos;
import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.Mention;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.mapper.IssueFlowMapper;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.MentionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final AppUserRepository appUserRepository;
    private final MentionRepository mentionRepository;
    private final IssueFlowMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(AppUserRepository appUserRepository, MentionRepository mentionRepository, IssueFlowMapper mapper, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.mentionRepository = mentionRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<UserDtos.UserResponse> all() { return appUserRepository.findAll().stream().map(mapper::toUser).toList(); }
    public UserDtos.UserResponse one(Long id) { return mapper.toUser(requireUser(id)); }

    public UserDtos.UserResponse create(UserDtos.CreateUserRequest req, String actor) {
        if (appUserRepository.existsByUsernameIgnoreCase(req.username())) throw new BadRequestException("Username already exists");
        if (appUserRepository.existsByEmailIgnoreCase(req.email())) throw new BadRequestException("Email already exists");
        AppUser u = new AppUser();
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setFullName(req.fullName());
        u.setRole(req.role());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        AppUser saved = appUserRepository.save(u);
        auditService.log(actor, "CREATE", "USER", saved.getId().toString(), null, saved.getUsername());
        return mapper.toUser(saved);
    }

    public void update(Long id, UserDtos.UpdateUserRequest req, String actor) {
        AppUser u = requireUser(id);
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.role() != null) u.setRole(req.role());
        appUserRepository.save(u);
        auditService.log(actor, "UPDATE", "USER", id.toString(), null, req.toString());
    }

    public void delete(Long id, String actor) {
        appUserRepository.delete(requireUser(id));
        auditService.log(actor, "DELETE", "USER", id.toString(), null, null);
    }

    public UserDtos.MentionPageResponse mentions(Long userId, int page, int pageSize) {
        var p = mentionRepository.findByUserIdOrderByCommentCreatedAtDesc(userId, PageRequest.of(Math.max(page - 1, 0), pageSize));
        List<CommentDtos.CommentResponse> data = p.getContent().stream().map(this::toCommentResponse).toList();
        return new UserDtos.MentionPageResponse(data, p.getTotalElements(), page);
    }

    private CommentDtos.CommentResponse toCommentResponse(Mention mention) {
        var comment = mention.getComment();
        var list = List.of(new CommonDtos.IdNameDto(mention.getUser().getId(), mention.getUser().getUsername(), mention.getUser().getFullName()));
        return new CommentDtos.CommentResponse(comment.getId(), comment.getTicket().getId(), comment.getAuthor().getId(),
                comment.getContent(), list, comment.getVersion());
    }

    public AppUser requireUser(Long id) {
        return appUserRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
