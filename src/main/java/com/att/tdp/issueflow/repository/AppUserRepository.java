package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    List<AppUser> findByRoleOrderByCreatedAtAsc(UserRole role);
}
