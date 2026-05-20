package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.UserDtos;
import com.att.tdp.issueflow.security.SecurityUtil;
import com.att.tdp.issueflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final SecurityUtil securityUtil;

    public UserController(UserService userService, SecurityUtil securityUtil) {
        this.userService = userService;
        this.securityUtil = securityUtil;
    }

    @GetMapping public List<UserDtos.UserResponse> all() { return userService.all(); }
    @GetMapping("/{userId}") public UserDtos.UserResponse one(@PathVariable Long userId) { return userService.one(userId); }
    @PostMapping public UserDtos.UserResponse create(@Valid @RequestBody UserDtos.CreateUserRequest request) { return userService.create(request, securityUtil.actor()); }
    @PostMapping("/update/{userId}") public void update(@PathVariable Long userId, @RequestBody UserDtos.UpdateUserRequest request) { userService.update(userId, request, securityUtil.actor()); }
    @DeleteMapping("/{userId}") public void delete(@PathVariable Long userId) { userService.delete(userId, securityUtil.actor()); }
    @GetMapping("/{userId}/mentions") public UserDtos.MentionPageResponse mentions(@PathVariable Long userId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) { return userService.mentions(userId, page, pageSize); }
}
