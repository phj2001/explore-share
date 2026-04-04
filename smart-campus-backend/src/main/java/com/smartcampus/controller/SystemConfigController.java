package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/system-configs")
@RequiredArgsConstructor
public class SystemConfigController {
    private static final CacheControl PUBLIC_CONFIG_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final SystemConfigService systemConfigService;

    @GetMapping("/public")
    public ResponseEntity<Result<Map<String, String>>> getPublicConfigs() {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CONFIG_CACHE)
                .body(Result.success(systemConfigService.getPublicConfigs()));
    }
}
