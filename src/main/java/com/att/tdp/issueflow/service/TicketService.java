package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.dto.TicketDtos;
import com.att.tdp.issueflow.entity.*;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.mapper.IssueFlowMapper;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.ProjectMemberRepository;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class TicketService {

    static final String SYSTEM_ACTOR = AuditService.SYSTEM_ACTOR;
    static final String AUTO_ASSIGN_ACTION = "AUTO_ASSIGN";
    static final String AUTO_ESCALATION_ACTION = "AUTO_ESCALATION";

    private final TicketRepository ticketRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final AppUserRepository appUserRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberService projectMemberService;
    private final TicketDependencyRepository dependencyRepository;
    private final IssueFlowMapper mapper;
    private final AuditService auditService;

    public TicketService(TicketRepository ticketRepository, ProjectService projectService, UserService userService,
                           AppUserRepository appUserRepository, ProjectMemberRepository projectMemberRepository,
                           ProjectMemberService projectMemberService, TicketDependencyRepository dependencyRepository,
                           IssueFlowMapper mapper, AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.appUserRepository = appUserRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectMemberService = projectMemberService;
        this.dependencyRepository = dependencyRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<TicketDtos.TicketResponse> byProject(Long projectId) {
        projectService.requireProject(projectId);
        return ticketRepository.findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(projectId).stream()
                .map(mapper::toTicket).toList();
    }

    public List<TicketDtos.TicketResponse> deleted(Long projectId) {
        projectService.requireAnyProject(projectId);
        return ticketRepository.findByProjectIdAndDeletedTrueOrderByCreatedAtDesc(projectId).stream()
                .map(mapper::toTicket).toList();
    }

    public TicketDtos.TicketResponse one(Long id) {
        return mapper.toTicket(requireTicket(id));
    }

    public TicketDtos.TicketResponse create(TicketDtos.CreateTicketRequest req, String actor) {
        Ticket ticket = new Ticket();
        ticket.setTitle(req.title());
        ticket.setDescription(req.description());
        ticket.setStatus(req.status());
        ticket.setPriority(req.priority());
        ticket.setType(req.type());
        ticket.setProject(projectService.requireProject(req.projectId()));
        ticket.setDueDate(req.dueDate());

        AppUser assignee = null;
        if (req.assigneeId() != null) {
            assignee = userService.requireUser(req.assigneeId());
        } else {
            assignee = selectLeastLoadedDeveloper(req.projectId());
        }
        ticket.setAssignee(assignee);
        if (assignee != null) {
            projectMemberService.linkDeveloper(ticket.getProject(), assignee);
        }

        Ticket saved = ticketRepository.save(ticket);
        auditService.log(actor, "CREATE", "TICKET", saved.getId().toString(), null, req.toString());
        if (req.assigneeId() == null && assignee != null) {
            auditService.log(SYSTEM_ACTOR, AUTO_ASSIGN_ACTION, "TICKET", saved.getId().toString(), null,
                    assignee.getId().toString());
        }
        return mapper.toTicket(saved);
    }

    public void update(Long id, TicketDtos.UpdateTicketRequest req, String actor) {
        Ticket ticket = requireTicket(id);
        if (ticket.getStatus() == TicketStatus.DONE) {
            throw new BadRequestException("DONE ticket cannot be updated");
        }
        if (req.version() != null && !req.version().equals(ticket.getVersion())) {
            throw new ConflictException("Ticket was modified by another user");
        }

        if (req.status() != null) {
            TicketStatusTransition.validate(ticket.getStatus(), req.status());
            if (req.status() == TicketStatus.DONE
                    && dependencyRepository.existsByTicketIdAndBlockedByStatusNot(id, TicketStatus.DONE)) {
                throw new BadRequestException("Ticket has open blockers");
            }
            ticket.setStatus(req.status());
        }
        if (req.title() != null) {
            ticket.setTitle(req.title());
        }
        if (req.description() != null) {
            ticket.setDescription(req.description());
        }
        if (req.priority() != null) {
            if (req.priority() != ticket.getPriority()) {
                ticket.setPriority(req.priority());
                ticket.setOverdue(false);
            }
        }
        if (req.assigneeId() != null) {
            AppUser assignee = userService.requireUser(req.assigneeId());
            ticket.setAssignee(assignee);
            projectMemberService.linkDeveloper(ticket.getProject(), assignee);
        }
        if (req.dueDate() != null) {
            ticket.setDueDate(req.dueDate());
        }
        ticketRepository.save(ticket);
        auditService.log(actor, "UPDATE", "TICKET", id.toString(), null, req.toString());
    }

    public void delete(Long id, String actor) {
        Ticket ticket = requireTicket(id);
        ticket.setDeleted(true);
        ticketRepository.save(ticket);
        auditService.log(actor, "SOFT_DELETE", "TICKET", id.toString(), null, null);
    }

    public void restore(Long id, String actor) {
        Ticket ticket = requireAnyTicket(id);
        ticket.setDeleted(false);
        ticketRepository.save(ticket);
        auditService.log(actor, "RESTORE", "TICKET", id.toString(), null, null);
    }

    public byte[] exportCsv(Long projectId) {
        List<Ticket> tickets = ticketRepository.findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(projectId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     "id", "title", "description", "status", "priority", "type", "assigneeId"))) {
            for (Ticket ticket : tickets) {
                printer.printRecord(
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getDescription(),
                        ticket.getStatus(),
                        ticket.getPriority(),
                        ticket.getType(),
                        ticket.getAssignee() == null ? null : ticket.getAssignee().getId());
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("CSV export failed");
        }
    }

    public TicketDtos.ImportResult importCsv(MultipartFile file, Long projectId, String actor) {
        int created = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            for (var record : parser) {
                try {
                    CreateTicketRequestFromCsv row = new CreateTicketRequestFromCsv(
                            record.get("title"),
                            record.get("description"),
                            TicketStatus.valueOf(record.get("status")),
                            TicketPriority.valueOf(record.get("priority")),
                            TicketType.valueOf(record.get("type")),
                            record.isMapped("assigneeId") && !record.get("assigneeId").isBlank()
                                    ? Long.parseLong(record.get("assigneeId")) : null
                    );
                    create(new TicketDtos.CreateTicketRequest(
                            row.title, row.description, row.status, row.priority, row.type,
                            projectId, row.assigneeId, null), actor);
                    created++;
                } catch (Exception ex) {
                    failed++;
                    errors.add(ex.getMessage());
                }
            }
            auditService.log(actor, "IMPORT", "TICKET", projectId.toString(), null,
                    "created=" + created + ",failed=" + failed);
            return new TicketDtos.ImportResult(created, failed, errors);
        } catch (IOException e) {
            throw new BadRequestException("CSV import failed");
        }
    }

    public Ticket requireTicket(Long id) {
        Ticket ticket = requireAnyTicket(id);
        if (ticket.isDeleted()) {
            throw new NotFoundException("Ticket not found");
        }
        return ticket;
    }

    public Ticket requireAnyTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new NotFoundException("Ticket not found"));
    }

    AppUser selectLeastLoadedDeveloper(Long projectId) {
        List<AppUser> developers = projectMemberRepository.findDevelopersByProjectId(projectId);
        if (developers.isEmpty()) {
            return null;
        }
        return developers.stream()
                .min(Comparator.<AppUser>comparingLong(dev ->
                                ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(
                                        projectId, dev.getId(), TicketStatus.DONE))
                        .thenComparing(AppUser::getCreatedAt))
                .orElse(null);
    }

    public void escalateOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> overdue = ticketRepository.findByDeletedFalseAndDueDateBeforeAndStatusNot(now, TicketStatus.DONE);
        for (Ticket ticket : overdue) {
            if (ticket.getDueDate() == null) {
                continue;
            }
            TicketPriority previous = ticket.getPriority();
            if (previous != TicketPriority.CRITICAL) {
                TicketPriority next = switch (previous) {
                    case LOW -> TicketPriority.MEDIUM;
                    case MEDIUM -> TicketPriority.HIGH;
                    case HIGH -> TicketPriority.CRITICAL;
                    default -> previous;
                };
                if (next != previous) {
                    ticket.setPriority(next);
                    auditService.log(SYSTEM_ACTOR, AUTO_ESCALATION_ACTION, "TICKET", ticket.getId().toString(),
                            previous.name(), next.name());
                }
            } else if (!ticket.isOverdue()) {
                ticket.setOverdue(true);
                auditService.log(SYSTEM_ACTOR, AUTO_ESCALATION_ACTION, "TICKET", ticket.getId().toString(),
                        "false", "true");
            }
        }
        ticketRepository.saveAll(overdue);
    }

    private record CreateTicketRequestFromCsv(String title, String description, TicketStatus status,
                                              TicketPriority priority, TicketType type, Long assigneeId) {
    }
}
