package com.att.tdp.issueflow;

import com.att.tdp.issueflow.entity.*;
import com.att.tdp.issueflow.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IssueFlowApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository projectMemberRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired TicketDependencyRepository ticketDependencyRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired MentionRepository mentionRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired RevokedTokenRepository revokedTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private Long adminId;
    private Long projectId;

    @BeforeEach
    void seedAndLogin() throws Exception {
        ticketDependencyRepository.deleteAll();
        mentionRepository.deleteAll();
        commentRepository.deleteAll();
        attachmentRepository.deleteAll();
        ticketRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        auditLogRepository.deleteAll();
        revokedTokenRepository.deleteAll();
        appUserRepository.deleteAll();

        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setEmail("admin@test.local");
        admin.setFullName("Admin");
        admin.setRole(UserRole.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("secret"));
        admin = appUserRepository.save(admin);
        adminId = admin.getId();

        Project project = new Project();
        project.setName("Integration Project");
        project.setDescription("Test");
        project.setOwner(admin);
        project = projectRepository.save(project);
        projectId = project.getId();

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        adminToken = body.get("accessToken").asText();
    }

    @Test
    void loginAndMe() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void createProject() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Project\",\"description\":\"Desc\",\"ownerId\":" + adminId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Project"));
    }

    @Test
    void createTicket() throws Exception {
        mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bug","description":"d","status":"TODO","priority":"LOW","type":"BUG","projectId":%d}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bug"));
    }

    @Test
    void rejectsInvalidStatusTransition() throws Exception {
        MvcResult created = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","description":"d","status":"TODO","priority":"LOW","type":"BUG","projectId":%d}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andReturn();
        long ticketId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void softDeletedTicketHiddenFromList() throws Exception {
        MvcResult created = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"DeleteMe","description":"d","status":"TODO","priority":"LOW","type":"BUG","projectId":%d}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andReturn();
        long ticketId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/tickets/" + ticketId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets").param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void dependencyBlocksDoneTransition() throws Exception {
        MvcResult blocker = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Blocker","description":"d","status":"TODO","priority":"LOW","type":"BUG","projectId":%d}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andReturn();
        long blockerId = objectMapper.readTree(blocker.getResponse().getContentAsString()).get("id").asLong();

        MvcResult blocked = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Blocked","description":"d","status":"TODO","priority":"LOW","type":"BUG","projectId":%d}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andReturn();
        long blockedId = objectMapper.readTree(blocked.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/tickets/" + blockedId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/tickets/" + blockedId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_REVIEW\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tickets/" + blockedId + "/dependencies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedBy\":" + blockerId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/tickets/" + blockedId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
    }
}
