package com.smartcampus.repository;

import com.smartcampus.entity.UserRouteLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRouteLikeRepository extends JpaRepository<UserRouteLike, Long> {

    boolean existsByRouteIdAndUserId(Long routeId, Long userId);

    void deleteByRouteIdAndUserId(Long routeId, Long userId);

    long countByRouteId(Long routeId);
}
