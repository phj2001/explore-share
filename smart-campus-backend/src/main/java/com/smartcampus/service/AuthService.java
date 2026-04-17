package com.smartcampus.service;

import com.smartcampus.dto.response.LoginResponse;
import com.smartcampus.entity.User;

import java.util.Optional;

public interface AuthService {

    /**
     * 用户注册
     */
    User register(User user);

    /**
     * 用户登录验证（返回包含token的响应）
     * @return 验证成功返回LoginResponse，失败返回空
     */
    Optional<LoginResponse> login(String username, String password);

    /**
     * 用户登录验证（仅验证）
     * @return 验证成功返回用户信息，失败返回空
     */
    Optional<User> verifyLogin(String username, String password);

    /**
     * 根据用户名获取用户
     */
    Optional<User> getUserByUsername(String username);

    /**
     * 根据ID获取用户
     */
    Optional<User> getUserById(Long id);

    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);

    /**
     * 登出：吊销当前用户的所有 token
     */
    void logout(Long userId);

    /**
     * 发送注册邮箱验证码
     */
    void sendRegisterCode(String email);

    /**
     * 发送重置密码验证码
     */
    void sendResetCode(String email);

    /**
     * 通过邮箱验证码重置密码
     */
    void resetPassword(String email, String code, String newPassword);

    /**
     * 注册（含邮箱验证码校验）
     */
    User register(User user, String emailCode);
}
