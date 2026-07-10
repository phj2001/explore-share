package com.smartcampus.repository;

import com.smartcampus.entity.NotificationEventOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 通知事件 Outbox Repository（升级项④）
 *
 * <p>替代智慧校园 MyBatis Mapper。关键查询用 @Query JPQL + Pageable 限量
 * （R2 修正：不用 ...NextRetryAtNullOr... 这种派生方法名，解析器有歧义）。
 */
@Repository
public interface NotificationEventOutboxRepository extends JpaRepository<NotificationEventOutbox, Long> {

    /**
     * 取待分发事件（PENDING 且到重试时间），nextRetryAt 为 null 的优先。
     */
    @Query("select e from NotificationEventOutbox e where e.deliveryStatus = :status " +
           "and (e.nextRetryAt is null or e.nextRetryAt <= :now) " +
           "order by case when e.nextRetryAt is null then 0 else 1 end, e.nextRetryAt asc, e.id asc")
    List<NotificationEventOutbox> findPendingForDispatch(@Param("status") int status,
                                                         @Param("now") OffsetDateTime now,
                                                         Pageable pageable);

    /**
     * 取失败事件（按 updatedAt asc, id asc），用于批量重试；排除已超 maxAttempts 的死信，
     * 避免管理接口把死信事件反复拉回重试（与定时任务 findRetryableFailed 保持一致的死信策略）。
     */
    @Query("select e from NotificationEventOutbox e where e.deliveryStatus = :status " +
           "and e.attemptCount < :maxAttempts " +
           "order by e.updatedAt asc, e.id asc")
    List<NotificationEventOutbox> findFailedForRetry(@Param("status") int status,
                                                     @Param("maxAttempts") int maxAttempts,
                                                     Pageable pageable);

    /**
     * 取「可自动重试」的失败事件（修复 #6）：FAILED、未超最大尝试次数、且已到下次重试时间。
     * 超过 maxAttempts 的事件不再被选中，留作死信由人工处理，避免无限重试。
     */
    @Query("select e from NotificationEventOutbox e where e.deliveryStatus = :status " +
           "and e.attemptCount < :maxAttempts " +
           "and (e.nextRetryAt is null or e.nextRetryAt <= :now) " +
           "order by e.nextRetryAt asc, e.id asc")
    List<NotificationEventOutbox> findRetryableFailed(@Param("status") int status,
                                                      @Param("maxAttempts") int maxAttempts,
                                                      @Param("now") OffsetDateTime now,
                                                      Pageable pageable);

    long countByDeliveryStatus(int deliveryStatus);
}
