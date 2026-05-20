package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @Query("""
            select pm.user from ProjectMember pm
            where pm.project.id = :projectId and pm.user.role = com.att.tdp.issueflow.entity.UserRole.DEVELOPER
            order by pm.user.createdAt asc
            """)
    List<AppUser> findDevelopersByProjectId(Long projectId);
}
