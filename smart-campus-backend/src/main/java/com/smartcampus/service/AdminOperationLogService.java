package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminOperationLogListItemResponse;

public interface AdminOperationLogService {

    void record(Long operatorUserId, String moduleName, String actionName, String targetType, Long targetId, String summary);

    PageResponse<AdminOperationLogListItemResponse> getLogs(String keyword, String moduleName, Integer page, Integer size);
}
