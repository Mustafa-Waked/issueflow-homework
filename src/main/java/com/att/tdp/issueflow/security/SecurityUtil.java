package com.att.tdp.issueflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    public String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }
}
