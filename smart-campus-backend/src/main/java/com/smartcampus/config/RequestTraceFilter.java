package com.smartcampus.config;

import com.smartcampus.util.RequestTraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求链路追踪过滤器（升级项①）
 *
 * <p>来源：智慧校园 RequestTraceFilter，仅改包名。每个请求解析/生成 traceId，
 * 写入 MDC（日志自动带上）与响应头 X-Trace-Id，请求结束清理 MDC 防止线程池串号。
 *
 * <p>执行顺序说明：作为 @Component 注册的 servlet filter，运行在 Spring Security
 * 过滤链之后、DispatcherServlet 之前——业务层（Controller/Service）日志均可拿到 traceId。
 */
@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = RequestTraceContext.resolveTraceId(request);
        request.setAttribute(RequestTraceContext.TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(RequestTraceContext.TRACE_ID_HEADER, traceId);
        MDC.put(RequestTraceContext.TRACE_ID_MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestTraceContext.TRACE_ID_MDC_KEY);
        }
    }
}
