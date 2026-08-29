package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.BindEmailRequest;
import com.smartcampus.dto.request.LoginRequest;
import com.smartcampus.dto.request.RegisterRequest;
import com.smartcampus.dto.request.ResetPasswordRequest;
import com.smartcampus.dto.request.SendEmailCodeRequest;
import com.smartcampus.dto.response.LoginResponse;
import com.smartcampus.dto.response.UserProfileResponse;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.security.UserRole;
import com.smartcampus.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        user.setEmail(request.getEmail());
        user.setRole(UserRole.USER.getCode());

        User registeredUser = authService.register(user, request.getEmailCode());
        return Result.success(UserProfileResponse.fromUser(registeredUser));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<LoginResponse> loginByJson(@Valid @RequestBody LoginRequest request) {
        return doLogin(request);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<LoginResponse> loginByForm(@Valid @ModelAttribute LoginRequest request) {
        return doLogin(request);
    }

    @PostMapping(value = "/login", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<LoginResponse> loginByMultipart(@Valid @ModelAttribute LoginRequest request) {
        return doLogin(request);
    }

    @GetMapping("/me")
    public Result<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            // 透传 HTTP 401：此前 Result.error(401) 走 @RestController 默认 200，状态码不一致
            throw new BusinessException(401, "未登录或登录已失效");
        }

        return authService.getUserById(userId)
                .map(UserProfileResponse::fromUser)
                .map(Result::success)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    @GetMapping("/user")
    public Result<UserProfileResponse> getUserByUsername(@RequestParam String username) {
        return authService.getUserByUsername(username)
                .map(UserProfileResponse::fromUser)
                .map(Result::success)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    @GetMapping("/user/{id}")
    public Result<UserProfileResponse> getUserById(@PathVariable Long id) {
        return authService.getUserById(id)
                .map(UserProfileResponse::fromUser)
                .map(Result::success)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    @GetMapping("/check")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        return Result.success(authService.existsByUsername(username));
    }

    /**
     * 登出：吊销当前用户所有 token
     */
    @DeleteMapping("/logout")
    public Result<Void> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            authService.logout(userId);
        }
        return Result.success(null);
    }

    /**
     * 发送注册邮箱验证码
     */
    @PostMapping("/sendRegisterCode")
    public Result<Void> sendRegisterCode(@Valid @RequestBody SendEmailCodeRequest request) {
        authService.sendRegisterCode(request.getEmail());
        return Result.success(null);
    }

    /**
     * 发送重置密码验证码
     */
    @PostMapping("/sendResetCode")
    public Result<Void> sendResetCode(@Valid @RequestBody SendEmailCodeRequest request) {
        authService.sendResetCode(request.getEmail());
        return Result.success(null);
    }

    /**
     * 通过邮箱验证码重置密码
     */
    @PostMapping("/resetPassword")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return Result.success(null);
    }

    /**
     * 发送补绑邮箱验证码（仅限未绑定邮箱的已登录账号）
     */
    @PostMapping("/sendBindCode")
    public Result<Void> sendBindCode(Authentication authentication, @Valid @RequestBody SendEmailCodeRequest request) {
        Long userId = requireUserId(authentication);
        authService.sendBindEmailCode(userId, request.getEmail().trim());
        return Result.success(null);
    }

    /**
     * 绑定邮箱：校验验证码后写入（仅限未绑定邮箱的账号，不支持换绑）
     */
    @PostMapping("/bindEmail")
    public Result<Void> bindEmail(Authentication authentication, @Valid @RequestBody BindEmailRequest request) {
        Long userId = requireUserId(authentication);
        authService.bindEmail(userId, request.getEmail().trim(), request.getEmailCode().trim());
        return Result.success(null);
    }

    private Result<LoginResponse> doLogin(LoginRequest request) {
        // 凭证错误抛 BusinessException(401)，由 GlobalExceptionHandler 透传为真实 HTTP 401，
        // 而非 @RestController 默认的 HTTP 200（与本次"业务码透传 HTTP 状态码"改造一致）。
        // 前端 login 请求已标记 skipAuthRedirect，不会因此在登录页触发"登录失效"跳转。
        return authService.login(request.getUsername(), request.getPassword())
                .map(Result::success)
                .orElseThrow(() -> new BusinessException(401, "用户名/邮箱或密码错误"));
    }

    /** 补绑邮箱端点位于已认证区（未加入 SecurityConfig 白名单），此处为防御性登录态校验 */
    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(401, "未登录或登录已失效");
        }
        return userId;
    }
}
