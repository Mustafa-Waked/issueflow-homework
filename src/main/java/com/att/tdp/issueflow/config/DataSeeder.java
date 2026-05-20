package com.att.tdp.issueflow.config;

import com.att.tdp.issueflow.entity.*;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.service.ProjectMemberService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    CommandLineRunner seed(AppUserRepository users, ProjectRepository projects, ProjectMemberService projectMembers,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            AppUser admin = user(users, passwordEncoder, "admin", "admin@issueflow.local", "Admin User", UserRole.ADMIN, "secret");
            AppUser dev1 = user(users, passwordEncoder, "jdoe", "jdoe@issueflow.local", "John Doe", UserRole.DEVELOPER, "secret");
            AppUser dev2 = user(users, passwordEncoder, "asmith", "asmith@issueflow.local", "Ann Smith", UserRole.DEVELOPER, "secret");

            Project project = new Project();
            project.setName("Sample Project");
            project.setDescription("Seeded project for local testing");
            project.setOwner(admin);
            project = projects.save(project);

            projectMembers.linkDeveloper(project, dev1);
            projectMembers.linkDeveloper(project, dev2);
        };
    }

    private AppUser user(AppUserRepository users, PasswordEncoder encoder, String username, String email,
                         String fullName, UserRole role, String password) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(encoder.encode(password));
        return users.save(user);
    }
}
