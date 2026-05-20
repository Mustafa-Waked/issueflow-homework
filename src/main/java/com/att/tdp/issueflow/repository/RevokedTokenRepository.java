package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByToken(String token);
}
