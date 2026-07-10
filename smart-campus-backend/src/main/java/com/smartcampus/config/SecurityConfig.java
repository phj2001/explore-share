package com.smartcampus.config;

import com.smartcampus.security.CustomAccessDeniedHandler;
import com.smartcampus.security.CustomAuthenticationEntryPoint;
import com.smartcampus.security.JwtAuthenticationFilter;
import com.smartcampus.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 升级项①可观测性（修复 #2）：放行健康检查与 Prometheus 抓取端点，
                        // 否则 anyRequest().authenticated() 会让 Prometheus 匿名抓取吃 401、Grafana 无数据。
                        // 生产建议进一步限制为内网/独立 management 端口。
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/check").permitAll()
                        .requestMatchers("/api/auth/sendRegisterCode", "/api/auth/sendResetCode", "/api/auth/resetPassword").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/activities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recommended-shares/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recommended-routes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/system-configs/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/poi-shares/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/poi-shares/*/tags").authenticated()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pois/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pois/*/check-in").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/pois/*/check-in").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/pois/*/favorite").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pois/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/pois/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/pois/*/rating").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pois/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pois/*/reviews").authenticated()
                        // 用户删除自己的评价：路径 DELETE /api/pois/reviews/{reviewId} 不匹配上面的 check-in/favorite 精确规则，
                        // 必须在通用 DELETE /api/pois/**（仅管理员）之前显式放行，否则普通用户永远无法删除自己的评价（403）
                        .requestMatchers(HttpMethod.DELETE, "/api/pois/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/shares").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/checkins").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/*/follow").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*/follow").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/follow-status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/following").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/followers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/feed").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/user-routes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user-routes/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user-routes").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/user-routes/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/user-routes/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/user-routes/*/like").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/user-routes/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me/routes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me/favorite-routes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/achievements").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me/achievements").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/achievements").permitAll()
                        .requestMatchers("/api/notifications/**").authenticated()
                        // AI 探索助手（AI 化升级）：登录用户可用；模块本身受 app.assistant.enabled 开关控制，默认关闭
                        .requestMatchers("/api/assistant/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/poi-applications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/poi-applications/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/pois/**").hasAnyRole(UserRole.SUPER_ADMIN.getRoleName(), UserRole.ADMIN.getRoleName())
                        .requestMatchers(HttpMethod.PUT, "/api/pois/**").hasAnyRole(UserRole.SUPER_ADMIN.getRoleName(), UserRole.ADMIN.getRoleName())
                        .requestMatchers(HttpMethod.DELETE, "/api/pois/**").hasAnyRole(UserRole.SUPER_ADMIN.getRoleName(), UserRole.ADMIN.getRoleName())
                        .requestMatchers(HttpMethod.PATCH, "/api/pois/**").hasAnyRole(UserRole.SUPER_ADMIN.getRoleName(), UserRole.ADMIN.getRoleName())
                        .requestMatchers("/api/routes/**").permitAll()
                        .requestMatchers("/api/admin/user-routes/**").hasAnyRole(UserRole.SUPER_ADMIN.getRoleName(), UserRole.ADMIN.getRoleName())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
