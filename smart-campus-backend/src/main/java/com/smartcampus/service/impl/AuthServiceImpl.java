package com.smartcampus.service.impl;

import com.smartcampus.dto.response.LoginResponse;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.JwtTokenProvider;
import com.smartcampus.security.UserRole;
import com.smartcampus.security.UserStatus;
import com.smartcampus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(User user) {
        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(409, "用户名已存在，请更换后重试");
        }

        user.setRole(UserRole.USER.getCode());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public Optional<LoginResponse> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    if (UserStatus.fromCode(user.getStatus()) == UserStatus.DISABLED) {
                        throw new BusinessException(403, "璐﹀彿宸茶绂佺敤");
                    }
                    String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
                    return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
                });
    }

    @Override
    public Optional<User> verifyLogin(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .filter(user -> UserStatus.fromCode(user.getStatus()) == UserStatus.ACTIVE);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}

