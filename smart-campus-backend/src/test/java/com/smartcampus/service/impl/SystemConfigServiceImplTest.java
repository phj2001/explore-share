package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AdminSystemConfigItemResponse;
import com.smartcampus.entity.SystemConfigEntry;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.SystemConfigEntryRepository;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.SystemConfigService;
import com.smartcampus.util.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统配置单元测试：管理员联系方式（文本类配置）新增项 + 删除 @NotBlank 后的空值回归。
 * 纯 Mockito，mock Repository / RedisUtils，不依赖数据库与 Redis 实例。
 */
class SystemConfigServiceImplTest {

    private SystemConfigEntryRepository repository;
    private RedisUtils redisUtils;
    private SystemConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SystemConfigEntryRepository.class);
        AdminOperationLogService adminOperationLogService = mock(AdminOperationLogService.class);
        redisUtils = mock(RedisUtils.class);
        // 缓存未命中（getObject 默认返回 null），findAll 默认空表 → 全部走 defaultValue
        when(repository.findAll()).thenReturn(List.of());
        service = new SystemConfigServiceImpl(repository, adminOperationLogService, redisUtils);
    }

    @Test
    void getPublicConfigs_包含管理员联系方式且默认为空串() {
        Map<String, String> result = service.getPublicConfigs();

        assertTrue(result.containsKey(SystemConfigService.ADMIN_CONTACT));
        assertEquals("", result.get(SystemConfigService.ADMIN_CONTACT));
        // 既有公开配置项不受影响
        assertTrue(result.containsKey(SystemConfigService.HOME_ANNOUNCEMENT_LIMIT));
    }

    @Test
    void updateConfig_文本配置_trim后保存并清缓存() {
        when(repository.findById(SystemConfigService.ADMIN_CONTACT)).thenReturn(Optional.empty());
        when(repository.save(any(SystemConfigEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminSystemConfigItemResponse response = service.updateConfig(
                SystemConfigService.ADMIN_CONTACT, "  admin@school.edu.cn  ", 1L);

        ArgumentCaptor<SystemConfigEntry> captor = ArgumentCaptor.forClass(SystemConfigEntry.class);
        verify(repository).save(captor.capture());
        assertEquals("admin@school.edu.cn", captor.getValue().getConfigValue());
        assertEquals("admin@school.edu.cn", response.getValue());
        // 配置变更后必须清缓存
        verify(redisUtils).delete("config:all");
    }

    @Test
    void updateConfig_文本配置_空值保存为空串() {
        // 删除 @NotBlank 后的清空路径：管理员可以把联系方式清回"未配置"态
        when(repository.findById(SystemConfigService.ADMIN_CONTACT)).thenReturn(Optional.empty());
        when(repository.save(any(SystemConfigEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminSystemConfigItemResponse response = service.updateConfig(
                SystemConfigService.ADMIN_CONTACT, "   ", 1L);

        ArgumentCaptor<SystemConfigEntry> captor = ArgumentCaptor.forClass(SystemConfigEntry.class);
        verify(repository).save(captor.capture());
        assertEquals("", captor.getValue().getConfigValue());
        assertEquals("", response.getValue());
    }

    @Test
    void updateConfig_整数配置_空值仍抛400() {
        // 回归：删除 @NotBlank 后，整数类配置的空值校验由 normalizer 兜底
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateConfig(SystemConfigService.HOME_ANNOUNCEMENT_LIMIT, "", 1L));

        assertEquals(400, ex.getCode());
        verify(repository, never()).save(any(SystemConfigEntry.class));
    }

    @Test
    void updateConfig_布尔配置_空值仍抛400() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateConfig(SystemConfigService.HOME_ANNOUNCEMENT_DEFAULT_COLLAPSED, null, 1L));

        assertEquals(400, ex.getCode());
        verify(repository, never()).save(any(SystemConfigEntry.class));
    }
}
