package com.smartcampus.service;

import com.smartcampus.dto.response.AdminSystemConfigItemResponse;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    String HOME_ANNOUNCEMENT_LIMIT = "home.announcement.limit";
    String HOME_ANNOUNCEMENT_DEFAULT_COLLAPSED = "home.announcement.defaultCollapsed";
    String DASHBOARD_HOT_POI_LIMIT = "dashboard.hotPoi.limit";
    String DASHBOARD_RECENT_SHARE_LIMIT = "dashboard.recentShare.limit";
    String ADMIN_CONTACT = "auth.adminContact";

    List<AdminSystemConfigItemResponse> getAdminConfigs();

    AdminSystemConfigItemResponse updateConfig(String configKey, String value, Long operatorUserId);

    Map<String, String> getPublicConfigs();

    int getIntValue(String configKey);

    boolean getBooleanValue(String configKey);
}
