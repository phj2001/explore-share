package com.smartcampus.repository;

import com.smartcampus.entity.UserRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {

    @EntityGraph(attributePaths = {"user", "waypoints"})
    Page<UserRoute> findByStatusOrderByCreatedAtDesc(Short status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "waypoints"})
    Page<UserRoute> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "waypoints"})
    Page<UserRoute> findByStatusAndUserIdNotOrderByCreatedAtDesc(Short status, Long userId, Pageable pageable);

    @Query("SELECT r FROM UserRoute r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.waypoints WHERE r.id = :id")
    UserRoute findWithDetailsById(@Param("id") Long id);

    long countByUserId(Long userId);

    @Query("SELECT r FROM UserRoute r JOIN FETCH r.user WHERE (:status IS NULL OR r.status = :status) AND (:keyword IS NULL OR :keyword = '' OR LOWER(r.title) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(r.summary) LIKE LOWER(CONCAT('%',:keyword,'%'))) ORDER BY r.createdAt DESC")
    Page<UserRoute> adminSearchRoutes(@Param("status") Short status, @Param("keyword") String keyword, Pageable pageable);

    /** 原子增减点赞数（避免并发下读-改-写丢失更新），结果不会小于 0。 */
    @Modifying
    @Query("UPDATE UserRoute r SET r.likeCount = CASE WHEN r.likeCount + :delta < 0 THEN 0 ELSE r.likeCount + :delta END WHERE r.id = :routeId")
    int adjustLikeCount(@Param("routeId") Long routeId, @Param("delta") int delta);

    /** 原子增减收藏数（避免并发下读-改-写丢失更新），结果不会小于 0。 */
    @Modifying
    @Query("UPDATE UserRoute r SET r.favoriteCount = CASE WHEN r.favoriteCount + :delta < 0 THEN 0 ELSE r.favoriteCount + :delta END WHERE r.id = :routeId")
    int adjustFavoriteCount(@Param("routeId") Long routeId, @Param("delta") int delta);
}
