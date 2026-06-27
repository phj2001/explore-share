package com.smartcampus.service;

/**
 * 登录风控服务（升级项③）
 *
 * <p>来源：智慧校园 LoginRiskControlService，仅改包名。提供登录失败计数与定时锁定，
 * 防御暴力破解。实现采用 Redis 优先 + 本地内存降级的双保险设计。
 */
public interface LoginRiskControlService {

    /**
     * 登录前置校验：若账号已锁定则抛出带剩余秒数提示的业务异常。
     */
    void assertLoginAllowed(String username);

    /**
     * 记录一次登录失败，达到阈值则锁定。
     */
    void recordFailure(String username);

    /**
     * 登录成功后清零失败计数与锁定状态。
     */
    void clearFailures(String username);
}
