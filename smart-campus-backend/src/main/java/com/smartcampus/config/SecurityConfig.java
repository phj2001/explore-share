package com.smartcampus.config;

import com.smartcampus.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器
     */
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
                .authorizeHttpRequests(auth -> auth
                        // CORS 预检请求 - 放行所有 OPTIONS 请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 公开接口 - 认证相关
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/check").permitAll()

                        // POI接口 - 按HTTP方法区分权限
                        // 查询操作（GET）- 公开
                        .requestMatchers(HttpMethod.GET, "/api/pois/**").permitAll()
                        // 增删改操作（POST/PUT/DELETE）- 需要认证
                        .requestMatchers(HttpMethod.POST, "/api/pois/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/pois/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/pois/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/pois/**").authenticated()

                        // 公开接口 - 路径规划
                        .requestMatchers("/api/routes/**").permitAll()

                        // 其他接口 - 需要认证
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
