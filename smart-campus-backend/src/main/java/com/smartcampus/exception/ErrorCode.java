package com.smartcampus.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用错误码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户相关错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    INVALID_PASSWORD(1003, "密码错误"),
    INVALID_TOKEN(1004, "令牌无效或已过期"),

    // POI相关错误码
    POI_NOT_FOUND(2001, "POI不存在"),
    INVALID_COORDINATES(2002, "坐标格式错误"),
    POI_CREATE_FAILED(2003, "POI创建失败"),

    // 路径规划相关错误码
    ROUTE_PLAN_FAILED(3001, "路径规划失败"),
    INVALID_ROUTE_PARAMS(3002, "路径规划参数错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
