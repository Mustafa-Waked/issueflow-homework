package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.AuthDtos;
import com.att.tdp.issueflow.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @SecurityRequirements
    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }
    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String auth) { authService.logout(auth); }
    @GetMapping("/me")
    public AuthDtos.MeResponse me(Authentication authentication) { return authService.me(authentication.getName()); }
}
