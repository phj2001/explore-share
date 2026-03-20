package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.RegisterRequest;
import com.smartcampus.dto.response.LoginResponse;
import com.smartcampus.dto.response.UserProfileResponse;
import com.smartcampus.entity.User;
import com.smartcampus.security.UserRole;
import com.smartcampus.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(UserRole.USER.getCode());

        User registeredUser = authService.register(user);
        return Result.success(UserProfileResponse.fromUser(registeredUser));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestParam String username, @RequestParam String password) {
        return authService.login(username, password)
                .map(Result::success)
                .orElse(Result.error("用户名或密码错误"));
    }

    @GetMapping("/me")
    public Result<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return Result.error(401, "未登录或登录已失效");
        }

        return authService.getUserById(userId)
                .map(UserProfileResponse::fromUser)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    @GetMapping("/user")
    public Result<UserProfileResponse> getUserByUsername(@RequestParam String username) {
        return authService.getUserByUsername(username)
                .map(UserProfileResponse::fromUser)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    @GetMapping("/user/{id}")
    public Result<UserProfileResponse> getUserById(@PathVariable Long id) {
        return authService.getUserById(id)
                .map(UserProfileResponse::fromUser)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    @GetMapping("/check")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        return Result.success(authService.existsByUsername(username));
    }
}
