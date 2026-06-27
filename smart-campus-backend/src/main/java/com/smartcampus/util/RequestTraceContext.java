package com.smartcampus.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 请求链路追踪上下文（升级项①）
 *
 * <p>来源：智慧校园 RequestTraceContext，仅改包名。jakarta 命名空间已对齐。
 * 提供 traceId 的生成 / 透传 / MDC 存取，供 {@link com.smartcampus.config.RequestTraceFilter}
 * 与业务日志、AOP 审计（升级项②）统一使用。
 */
public final class RequestTraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_ATTRIBUTE = "requestTraceId";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private RequestTraceContext() {
    }

    public static String resolveTraceId(HttpServletRequest request) {
        if (request == null) {
            return createTraceId();
        }
        String incomingTraceId = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(incomingTraceId)) {
            return incomingTraceId.trim();
        }
        Object traceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        if (traceId instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        return createTraceId();
    }

    public static String getCurrentTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "";
        }
        Object requestTraceId = attributes.getRequest().getAttribute(TRACE_ID_ATTRIBUTE);
        return requestTraceId instanceof String text ? text : "";
    }

    public static String prependTraceId(String message) {
        String traceId = getCurrentTraceId();
        if (!StringUtils.hasText(traceId)) {
            return message;
        }
        String normalizedMessage = StringUtils.hasText(message) ? message : "无附加消息";
        return "[traceId=" + traceId + "] " + normalizedMessage;
    }

    public static String createTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
