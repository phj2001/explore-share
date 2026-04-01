package com.smartcampus.repository;

import com.smartcampus.entity.RecommendedRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendedRouteRepository extends JpaRepository<RecommendedRoute, Long>, JpaSpecificationExecutor<RecommendedRoute> {

    Optional<RecommendedRoute> findByIdAndStatus(Long id, Short status);

    List<RecommendedRoute> findByStatusOrderBySortOrderAscPublishedAtDescIdDesc(Short status);
}
