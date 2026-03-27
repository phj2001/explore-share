package com.smartcampus.repository;

import com.smartcampus.entity.POIShare;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface POIShareRepository extends JpaRepository<POIShare, Long>, JpaSpecificationExecutor<POIShare> {

    @Override
    @EntityGraph(attributePaths = {"poi", "user", "images"})
    Page<POIShare> findAll(Specification<POIShare> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    Page<POIShare> findByPoiId(Long poiId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    Optional<POIShare> findWithUserAndImagesById(Long id);

    @EntityGraph(attributePaths = {"poi", "user", "images"})
    Optional<POIShare> findWithPoiUserAndImagesById(Long id);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @EntityGraph(attributePaths = {"poi", "user", "images"})
    List<POIShare> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime createdAt);

    @EntityGraph(attributePaths = {"poi", "user", "images"})
    List<POIShare> findTop5ByOrderByCreatedAtDesc();

    @Query("select s.poi.id, count(s.id) from POIShare s where s.createdAt >= :start group by s.poi.id")
    List<Object[]> countGroupedByPoiIdsSince(@Param("start") LocalDateTime start);

    long countByUserId(Long userId);
}
