package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.ProjectMember;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.repository.ProjectMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    public void linkDeveloper(Project project, AppUser user) {
        if (user == null || user.getRole() != UserRole.DEVELOPER) {
            return;
        }
        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId())) {
            return;
        }
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        projectMemberRepository.save(member);
    }
}
