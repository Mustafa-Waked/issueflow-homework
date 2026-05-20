package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.*;
import com.att.tdp.issueflow.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IssueFlowMapper {
    public UserDtos.UserResponse toUser(AppUser user) {
        return new UserDtos.UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public ProjectDtos.ProjectResponse toProject(Project project) {
        return new ProjectDtos.ProjectResponse(project.getId(), project.getName(), project.getDescription(), project.getOwner().getId());
    }

    public TicketDtos.TicketResponse toTicket(Ticket ticket) {
        return new TicketDtos.TicketResponse(
                ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getStatus(), ticket.getPriority(), ticket.getType(),
                ticket.getProject().getId(), ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
                ticket.getDueDate(), ticket.isOverdue(), ticket.getVersion()
        );
    }

    public CommentDtos.CommentResponse toComment(Comment comment, List<CommonDtos.IdNameDto> mentions) {
        return new CommentDtos.CommentResponse(comment.getId(), comment.getTicket().getId(), comment.getAuthor().getId(),
                comment.getContent(), mentions, comment.getVersion());
    }

}
