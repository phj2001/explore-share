package com.smartcampus.aspect;

import com.smartcampus.annotation.OperationLog;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.util.RequestTraceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作审计切面（升级项②）
 *
 * <p>注解驱动：拦截所有标注 {@link OperationLog} 的方法，自动落库到 AdminOperationLog。
 *
 * <p>设计要点：
 * <ul>
 *   <li>切点 {@code @annotation(operationLog)}：注解驱动，零命名耦合（本项目 Controller 平铺无子包，不能按包名匹配）。</li>
 *   <li>当前用户：从 {@link SecurityContextHolder} 取 Long userId（JwtAuthenticationFilter 注入 principal）。</li>
 *   <li>业务参数：SpEL 表达式从方法入参提取 targetId / summary。</li>
 *   <li>请求元数据：HTTP method / URI / IP 从 HttpServletRequest 取。</li>
 *   <li>traceId：复用升级项① 的 {@link RequestTraceContext}，实现审计与日志/指标串联。</li>
 *   <li>事务：调用 {@code record(12 参数)} 的 REQUIRES_NEW 重载，失败记录独立提交，不受主业务回滚影响。</li>
 *   <li>容错：审计落库失败仅记 warn 日志，不阻断主业务；异常分支 finally 落库后 re-throw 原异常。</li>
 * </ul>
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer PARAM_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final AdminOperationLogService adminOperationLogService;

    public OperationLogAspect(AdminOperationLogService adminOperationLogService) {
        this.adminOperationLogService = adminOperationLogService;
    }

    @Around("@annotation(operationLog)")
    public Object recordOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Long operatorUserId = resolveCurrentUserId();
        HttpServletRequest request = resolveRequest();
        EvaluationContext spelContext = buildSpelContext(joinPoint);

        boolean success = false;
        Throwable error = null;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            Integer operationStatus = success ? 1 : 0;
            try {
                persistLog(operationLog, operatorUserId, request, spelContext, operationStatus, durationMs, error);
            } catch (Exception logEx) {
                log.warn("操作审计落库失败 action={} traceId={}: {}",
                        operationLog.action(), RequestTraceContext.getCurrentTraceId(), logEx.getMessage());
            }
        }
    }

    private void persistLog(OperationLog operationLog, Long operatorUserId, HttpServletRequest request,
                            EvaluationContext spelContext, Integer operationStatus, long durationMs, Throwable error) {
        if (operatorUserId == null) {
            // 非 HTTP 请求链路（SecurityContext 空）：跳过，保护注解误用
            return;
        }

        String targetIdStr = evalSpel(operationLog.targetIdSpel(), spelContext, String.class);
        Long targetId = parseTargetId(targetIdStr);

        String summary = StringUtils.hasText(operationLog.summarySpel())
                ? evalSpel(operationLog.summarySpel(), spelContext, String.class)
                : operationLog.action();
        if (!StringUtils.hasText(summary)) {
            summary = operationLog.action();
        }
        // 失败时把异常信息拼入 summary，便于排查
        if (error != null && error.getMessage() != null) {
            String errSummary = summary + " | 失败: " + error.getMessage();
            summary = errSummary.length() > 300 ? errSummary.substring(0, 297) + "..." : errSummary;
        }

        String requestMethod = request != null ? request.getMethod() : null;
        String requestUri = request != null ? request.getRequestURI() : null;
        String ipAddress = request != null ? resolveClientIp(request) : null;
        String traceId = RequestTraceContext.getCurrentTraceId();
        if (!StringUtils.hasText(traceId)) {
            traceId = null;
        }

        adminOperationLogService.record(
                operatorUserId,
                operationLog.module(),
                operationLog.action(),
                operationLog.targetType(),
                targetId,
                summary,
                requestMethod,
                requestUri,
                ipAddress,
                operationStatus,
                traceId,
                durationMs);
    }

    private Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        // JwtAuthenticationFilter 把 Long userId 作为 principal
        return principal instanceof Long userId ? userId : null;
    }

    private HttpServletRequest resolveRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private EvaluationContext buildSpelContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = PARAM_NAME_DISCOVERER.getParameterNames(method);
        Object[] args = joinPoint.getArgs();
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        return context;
    }

    private <T> T evalSpel(String expression, EvaluationContext context, Class<T> resultType) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        try {
            Expression exp = SPEL_PARSER.parseExpression(expression);
            return exp.getValue(context, resultType);
        } catch (Exception ex) {
            // SpEL 求值失败（表达式错误/参数缺失）不阻断审计
            return null;
        }
    }

    private Long parseTargetId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
