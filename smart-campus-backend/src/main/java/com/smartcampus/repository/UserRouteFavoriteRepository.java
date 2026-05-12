package com.smartcampus.repository;

import com.smartcampus.entity.UserRouteFavorite;
import com.smartcampus.entity.UserRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRouteFavoriteRepository extends JpaRepository<UserRouteFavorite, Long> {

    boolean existsByRouteIdAndUserId(Long routeId, Long userId);

    void deleteByRouteIdAndUserId(Long routeId, Long userId);

    long countByRouteId(Long routeId);

    @EntityGraph(attributePaths = {"route", "route.user", "route.waypoints"})
    Page<UserRouteFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
