package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Mention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MentionRepository extends JpaRepository<Mention, Long> {
    @Query("""
            select m from Mention m
            join fetch m.comment c
            join fetch c.ticket
            join fetch c.author
            join fetch m.user
            where m.user.id = :userId
            order by c.createdAt desc
            """)
    Page<Mention> findByUserIdOrderByCommentCreatedAtDesc(Long userId, Pageable pageable);
    List<Mention> findByCommentId(Long commentId);
    void deleteByCommentId(Long commentId);
}
