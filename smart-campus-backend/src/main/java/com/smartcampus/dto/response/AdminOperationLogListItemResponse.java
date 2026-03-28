package com.smartcampus.dto.response;

import com.smartcampus.entity.AdminOperationLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperationLogListItemResponse {

    private Long id;
    private Long operatorUserId;
    private String operatorDisplayName;
    private String operatorUsername;
    private String moduleName;
    private String actionName;
    private String targetType;
    private Long targetId;
    private String summary;
    private LocalDateTime createdAt;

    public static AdminOperationLogListItemResponse fromEntity(AdminOperationLog entity) {
        return new AdminOperationLogListItemResponse(
                entity.getId(),
                entity.getOperatorUserId(),
                entity.getOperatorDisplayName(),
                entity.getOperatorUsername(),
                entity.getModuleName(),
                entity.getActionName(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getSummary(),
                entity.getCreatedAt()
        );
    }
}
