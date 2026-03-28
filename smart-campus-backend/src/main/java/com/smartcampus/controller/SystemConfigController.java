package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system-configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/public")
    public Result<Map<String, String>> getPublicConfigs() {
        return Result.success(systemConfigService.getPublicConfigs());
    }
}
