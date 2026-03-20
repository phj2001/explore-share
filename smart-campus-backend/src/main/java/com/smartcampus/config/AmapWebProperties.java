package com.smartcampus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "amap.web")
public class AmapWebProperties {

    /**
     * 高德 Web 服务 Key
     */
    private String key;

    /**
     * 高德 Web 服务基础地址
     */
    private String baseUrl = "https://restapi.amap.com";
}
