package com.smartcampus.repository;

import com.smartcampus.entity.RecommendedRouteWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendedRouteWaypointRepository extends JpaRepository<RecommendedRouteWaypoint, Long> {

    List<RecommendedRouteWaypoint> findByPoiId(Long poiId);
}
