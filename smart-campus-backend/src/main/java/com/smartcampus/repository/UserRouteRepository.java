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
}
