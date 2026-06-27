package com.smartcampus.repository;

import com.smartcampus.entity.POIReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface POIReviewRepository extends JpaRepository<POIReview, Long>, JpaSpecificationExecutor<POIReview> {

    Optional<POIReview> findByPoiIdAndUserId(Long poiId, Long userId);

    long countByPoiId(Long poiId);

    Page<POIReview> findByPoiIdOrderByCreatedAtDesc(Long poiId, Pageable pageable);

    Page<POIReview> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(CAST(r.rating AS double)), 0) FROM POIReview r WHERE r.poi.id = :poiId")
    double getAverageRatingByPoiId(@Param("poiId") Long poiId);

    @Query("SELECT r.poi.id, COALESCE(AVG(CAST(r.rating AS double)), 0), COUNT(r.id) " +
            "FROM POIReview r WHERE r.poi.id IN :poiIds GROUP BY r.poi.id")
    List<Object[]> getRatingSummaryByPoiIds(@Param("poiIds") List<Long> poiIds);

    Page<POIReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByUserId(Long userId);
}
