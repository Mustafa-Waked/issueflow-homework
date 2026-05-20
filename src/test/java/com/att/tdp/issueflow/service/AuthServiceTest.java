package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.AuthDtos;
import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.exception.UnauthorizedException;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.RevokedTokenRepository;
import com.att.tdp.issueflow.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Test
    void shouldRejectInvalidLogin() {
        AppUserRepository users = Mockito.mock(AppUserRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);
        RevokedTokenRepository revoked = Mockito.mock(RevokedTokenRepository.class);
        AuthService service = new AuthService(users, encoder, jwtService, revoked);
        AppUser user = new AppUser();
        user.setUsername("jdoe");
        user.setRole(UserRole.DEVELOPER);
        user.setPasswordHash("hash");
        Mockito.when(users.findByUsernameIgnoreCase("jdoe")).thenReturn(Optional.of(user));
        Mockito.when(encoder.matches("bad", "hash")).thenReturn(false);
        Assertions.assertThrows(UnauthorizedException.class, () -> service.login(new AuthDtos.LoginRequest("jdoe", "bad")));
    }
}
