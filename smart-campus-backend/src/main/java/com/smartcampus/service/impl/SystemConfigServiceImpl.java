package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AdminSystemConfigItemResponse;
import com.smartcampus.entity.SystemConfigEntry;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.SystemConfigEntryRepository;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.SystemConfigService;
import com.smartcampus.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Map<String, ConfigDefinition> DEFINITIONS = buildDefinitions();
    private static final String CONFIG_CACHE_KEY = "config:all";
    private static final long   CONFIG_CACHE_TTL = 600L; // 10 分钟

    private final SystemConfigEntryRepository systemConfigEntryRepository;
    private final AdminOperationLogService adminOperationLogService;
    private final RedisUtils redisUtils;

    @Override
    @Transactional(readOnly = true)
    public List<AdminSystemConfigItemResponse> getAdminConfigs() {
        Map<String, String> values = loadValueMap();
        return DEFINITIONS.values().stream()
                .map(definition -> toResponse(definition, values.get(definition.configKey())))
                .toList();
    }

    @Override
    @Transactional
    public AdminSystemConfigItemResponse updateConfig(String configKey, String value, Long operatorUserId) {
        ConfigDefinition definition = getRequiredDefinition(configKey);
        String normalizedValue = definition.normalizer().apply(value);

        SystemConfigEntry entry = systemConfigEntryRepository.findById(definition.configKey())
                .orElseGet(SystemConfigEntry::new);
        entry.setConfigKey(definition.configKey());
        entry.setConfigValue(normalizedValue);
        systemConfigEntryRepository.save(entry);

        adminOperationLogService.record(
                operatorUserId,
                "系统配置",
                "更新系统配置",
                "配置项",
                null,
                "更新配置 " + definition.label() + " 为 " + normalizedValue
        );

        // 配置变更后清除缓存
        redisUtils.delete(CONFIG_CACHE_KEY);

        return toResponse(definition, normalizedValue);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getPublicConfigs() {
        Map<String, String> values = loadValueMap();
        Map<String, String> result = new LinkedHashMap<>();
        for (ConfigDefinition definition : DEFINITIONS.values()) {
            if (definition.publicVisible()) {
                result.put(definition.configKey(), resolveValue(definition, values.get(definition.configKey())));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public int getIntValue(String configKey) {
        ConfigDefinition definition = getRequiredDefinition(configKey);
        return Integer.parseInt(resolveValue(definition, loadValueMap().get(configKey)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBooleanValue(String configKey) {
        ConfigDefinition definition = getRequiredDefinition(configKey);
        return Boolean.parseBoolean(resolveValue(definition, loadValueMap().get(configKey)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadValueMap() {
        // 先查 Redis 缓存
        Map<String, String> cached = redisUtils.getObject(CONFIG_CACHE_KEY, LinkedHashMap.class);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中：查数据库并写入缓存
        Map<String, String> valueMap = systemConfigEntryRepository.findAll().stream()
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getConfigKey(), entry.getConfigValue()), Map::putAll);
        redisUtils.setObject(CONFIG_CACHE_KEY, valueMap, CONFIG_CACHE_TTL, TimeUnit.SECONDS);
        return valueMap;
    }

    private AdminSystemConfigItemResponse toResponse(ConfigDefinition definition, String currentValue) {
        return new AdminSystemConfigItemResponse(
                definition.configKey(),
                definition.label(),
                definition.description(),
                definition.valueType(),
                resolveValue(definition, currentValue),
                definition.defaultValue(),
                definition.publicVisible()
        );
    }

    private String resolveValue(ConfigDefinition definition, String currentValue) {
        return StringUtils.hasText(currentValue) ? currentValue : definition.defaultValue();
    }

    private ConfigDefinition getRequiredDefinition(String configKey) {
        ConfigDefinition definition = DEFINITIONS.get(configKey);
        if (definition == null) {
            throw new BusinessException(404, "配置项不存在");
        }
        return definition;
    }

    private static Map<String, ConfigDefinition> buildDefinitions() {
        Map<String, ConfigDefinition> definitions = new LinkedHashMap<>();
        definitions.put(SystemConfigService.HOME_ANNOUNCEMENT_LIMIT, new ConfigDefinition(
                SystemConfigService.HOME_ANNOUNCEMENT_LIMIT,
                "首页公告数量",
                "控制首页公告区默认拉取的公告条数，范围 1 到 12。",
                "INTEGER",
                "8",
                true,
                value -> normalizeInt(value, 1, 12, "首页公告数量必须是 1 到 12 的整数")
        ));
        definitions.put(SystemConfigService.HOME_ANNOUNCEMENT_DEFAULT_COLLAPSED, new ConfigDefinition(
                SystemConfigService.HOME_ANNOUNCEMENT_DEFAULT_COLLAPSED,
                "首页公告默认收起",
                "控制桌面端首页左侧公告栏首次进入时是否默认收起。",
                "BOOLEAN",
                "false",
                true,
                value -> normalizeBoolean(value, "首页公告默认收起仅支持 true 或 false")
        ));
        definitions.put(SystemConfigService.DASHBOARD_HOT_POI_LIMIT, new ConfigDefinition(
                SystemConfigService.DASHBOARD_HOT_POI_LIMIT,
                "热门 POI 榜单数量",
                "控制运营总览热门 POI 排行模块的展示数量，范围 1 到 20。",
                "INTEGER",
                "10",
                false,
                value -> normalizeInt(value, 1, 20, "热门 POI 榜单数量必须是 1 到 20 的整数")
        ));
        definitions.put(SystemConfigService.DASHBOARD_RECENT_SHARE_LIMIT, new ConfigDefinition(
                SystemConfigService.DASHBOARD_RECENT_SHARE_LIMIT,
                "最近分享展示数量",
                "控制运营总览最近新增分享模块的展示数量，范围 1 到 20。",
                "INTEGER",
                "5",
                false,
                value -> normalizeInt(value, 1, 20, "最近分享展示数量必须是 1 到 20 的整数")
        ));
        definitions.put(SystemConfigService.ADMIN_CONTACT, new ConfigDefinition(
                SystemConfigService.ADMIN_CONTACT,
                "管理员联系方式",
                "展示在登录页忘记密码流程中，供未绑定邮箱的用户联系管理员重置密码；留空表示未配置，前端不展示联系方式。",
                "STRING",
                "",
                true,
                SystemConfigServiceImpl::normalizeText
        ));
        return definitions;
    }

    private static String normalizeInt(String value, int min, int max, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new BusinessException(400, message);
            }
            return String.valueOf(parsed);
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, message);
        }
    }

    private static String normalizeBoolean(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        String normalized = value.trim().toLowerCase();
        if (!"true".equals(normalized) && !"false".equals(normalized)) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }

    /**
     * 文本类配置归一化：null/空白返回空串（允许管理员清空配置，"清空"与"未配置"统一收敛为空串），
     * 否则 trim 后限制长度。
     */
    private static String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > 255) {
            throw new BusinessException(400, "文本长度不能超过255个字符");
        }
        return trimmed;
    }

    private record ConfigDefinition(
            String configKey,
            String label,
            String description,
            String valueType,
            String defaultValue,
            boolean publicVisible,
            Function<String, String> normalizer
    ) {
    }
}
