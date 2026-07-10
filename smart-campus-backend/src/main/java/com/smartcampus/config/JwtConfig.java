package com.smartcampus.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 密钥：必须通过环境变量 JWT_SECRET 配置，至少 32 字节（HS256 要求）；
     * 未配置或过短将在启动期直接失败，避免运行期首次签发 token 时抛 WeakKeyException 后由全局异常处理泄露堆栈。
     */
    @NotBlank(message = "JWT 密钥未配置(jwt.secret)，请通过环境变量 JWT_SECRET 设置")
    @Size(min = 32, message = "JWT 密钥长度不足，至少需要 32 字节(HS256 要求)")
    private String secret;

    /**
     * JWT 过期时间（毫秒）
     */
    @NotNull(message = "JWT 过期时间(jwt.expiration)未配置")
    private Long expiration;
}
