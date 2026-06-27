package com.smartcampus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式操作审计注解（升级项②）
 *
 * <p>加在 Service 入口方法上，由 {@link com.smartcampus.aspect.OperationLogAspect} 切面接管，
 * 自动落库到 {@code AdminOperationLog}。SpEL 表达式从方法参数提取 targetId / summary，
 * 当前用户、IP、traceId、成功失败、耗时由切面统一补齐。
 *
 * <p><b>适用边界</b>：仅加在 HTTP 请求链路触发的 Service 入口方法（SecurityContext 可用）；
 * 被 Service 内部调用的方法不要加（取不到当前用户，记录会被跳过）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 模块名（如 "内容管理"） */
    String module();

    /** 动作名（如 "删除分享"） */
    String action();

    /** 目标类型（如 "分享"） */
    String targetType();

    /** 目标 ID 的 SpEL 表达式（如 "#shareId"），空则不记录 targetId */
    String targetIdSpel() default "";

    /** 摘要的 SpEL 表达式（如 "'删除分享 #' + #shareId"），空则用 {@link #action()} */
    String summarySpel() default "";
}
