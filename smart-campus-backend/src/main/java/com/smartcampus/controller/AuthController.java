package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.LoginResponse;
import com.smartcampus.entity.User;
import com.smartcampus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        User registeredUser = authService.register(user);
        return Result.success(registeredUser);
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestParam String username, @RequestParam String password) {
        return authService.login(username, password)
                .map(Result::success)
                .orElse(Result.error("用户名或密码错误"));
    }

    /**
     * 根据用户名获取用户
     * GET /api/auth/user?username=xxx
     */
    @GetMapping("/user")
    public Result<User> getUserByUsername(@RequestParam String username) {
        return authService.getUserByUsername(username)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    /**
     * 根据ID获取用户
     * GET /api/auth/user/{id}
     */
    @GetMapping("/user/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return authService.getUserById(id)
                .map(Result::success)
                .orElse(Result.error(404, "用户不存在"));
    }

    /**
     * 检查用户名是否存在
     * GET /api/auth/check?username=xxx
     */
    @GetMapping("/check")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        return Result.success(authService.existsByUsername(username));
    }
}
