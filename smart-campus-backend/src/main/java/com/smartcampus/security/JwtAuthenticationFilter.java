package com.smartcampus.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.dto.common.Result;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.util.RedisUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /** 用户信息缓存 TTL（分钟） */
    private static final long USER_CACHE_TTL_MINUTES = 5L;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            // P0：JWT 黑名单检查（revoke_before 方案）
            // 逻辑：登出或改密时设置 jwt:revoke_before:{userId} = 当前时间戳(毫秒)
            // 若 token 签发时间 < revoke_before - 1s，则该 token 已被吊销。
            // ⚠️ 这里减 1000ms 是必要的宽限：JWT 的 iat 精度只到“秒”(getIssuedAt().getTime() 已被截断到整秒)，
            //    而 revoke_before 是完整毫秒；若用 <=，会把“登出后同一秒内立刻重新登录”签发的新 token
            //    （其秒级 iat 恰好 ≤ 登出毫秒值）误判为已吊销，导致用户重登后仍被 401。1 秒宽限消除此误杀。
            String revokeKey = "jwt:revoke_before:" + userId;
            String revokeBeforeStr = redisUtils.get(revokeKey);
            if (revokeBeforeStr != null) {
                long issuedAt = jwtTokenProvider.getIssuedAtFromToken(token);
                if (issuedAt < Long.parseLong(revokeBeforeStr) - 1000) {
                    // token 已被吊销，视为未登录（不报错，让后续鉴权逻辑处理）
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // P1：用户信息缓存（status:role 格式，5 分钟 TTL）
            Short status;
            Short role;
            String cacheKey = "user:info:" + userId;
            String cachedInfo = redisUtils.get(cacheKey);

            if (cachedInfo != null) {
                // 命中缓存：格式 "status:role"
                String[] parts = cachedInfo.split(":");
                status = Short.parseShort(parts[0]);
                role = Short.parseShort(parts[1]);
            } else {
                // 缓存未命中：查数据库并写入缓存
                var user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    filterChain.doFilter(request, response);
                    return;
                }
                status = user.getStatus();
                role = user.getRole();
                redisUtils.set(cacheKey, status + ":" + role, USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }

            UserStatus userStatus = UserStatus.fromCode(status);
            if (userStatus == UserStatus.DISABLED) {
                writeUnauthorizedResponse(response, "账号已被禁用");
                return;
            }
            if (userStatus == UserStatus.CANCELLED) {
                writeUnauthorizedResponse(response, "账号已注销");
                return;
            }

            setAuthentication(userId, role);
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(Long userId, Short role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(UserRole.fromCode(role).toAuthority())
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, message)));
    }
}
