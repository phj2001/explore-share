package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminOperationLogListItemResponse;

public interface AdminOperationLogService {

    void record(Long operatorUserId, String moduleName, String actionName, String targetType, Long targetId, String summary);

    /**
     * 带请求级元数据的重载（升级项② AOP 切面调用）。
     * 实现用 REQUIRES_NEW 独立事务，确保失败记录不受主业务回滚影响；
     * 扩展字段为 null 时保留实体默认 null，向后兼容。
     */
    void record(Long operatorUserId, String moduleName, String actionName, String targetType, Long targetId, String summary,
                String requestMethod, String requestUri, String ipAddress, Integer operationStatus, String traceId, Long durationMs);

    PageResponse<AdminOperationLogListItemResponse> getLogs(String keyword, String moduleName, Integer page, Integer size);
}
