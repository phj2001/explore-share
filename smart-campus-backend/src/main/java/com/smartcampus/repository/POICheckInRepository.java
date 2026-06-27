package com.smartcampus.repository;

import com.smartcampus.entity.POICheckIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface POICheckInRepository extends JpaRepository<POICheckIn, Long> {

    boolean existsByPoiIdAndUserId(Long poiId, Long userId);

    Optional<POICheckIn> findByPoiIdAndUserId(Long poiId, Long userId);

    long countByPoiId(Long poiId);

    long countByUserId(Long userId);

    Page<POICheckIn> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT DISTINCT c.poi.category FROM POICheckIn c WHERE c.user.id = :userId")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT c.poi.category) FROM POICheckIn c WHERE c.user.id = :userId")
    long countDistinctCategoriesByUserId(@Param("userId") Long userId);

    @Query("SELECT c.user.id, COUNT(c.id) FROM POICheckIn c GROUP BY c.user.id ORDER BY COUNT(c.id) DESC")
    List<Object[]> countGroupedByUserIdDesc();

    @Query("SELECT c.user.id, COUNT(c.id) FROM POICheckIn c WHERE c.createdAt >= :start GROUP BY c.user.id ORDER BY COUNT(c.id) DESC")
    List<Object[]> countGroupedByUserIdSinceDesc(@Param("start") java.time.LocalDateTime start);

    @Query("SELECT c.poi.id, c.poi.name, c.poi.category, c.poi.latitude, c.poi.longitude, COUNT(c.id) " +
            "FROM POICheckIn c GROUP BY c.poi.id, c.poi.name, c.poi.category, c.poi.latitude, c.poi.longitude " +
            "ORDER BY COUNT(c.id) DESC")
    List<Object[]> countGroupedByPoiIdDesc();
}
