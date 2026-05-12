package com.smartcampus.repository;

import com.smartcampus.entity.POIFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface POIFavoriteRepository extends JpaRepository<POIFavorite, Long> {

    boolean existsByPoiIdAndUserId(Long poiId, Long userId);

    Optional<POIFavorite> findByPoiIdAndUserId(Long poiId, Long userId);

    long countByPoiId(Long poiId);

    void deleteByPoiIdAndUserId(Long poiId, Long userId);

    Page<POIFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select f.poi.id from POIFavorite f where f.user.id = :userId")
    List<Long> findPoiIdByUserId(@Param("userId") Long userId);

    @Query("select f.poi.id, count(f.id) from POIFavorite f where f.poi.id in :poiIds group by f.poi.id")
    List<Object[]> countGroupedByPoiIds(@Param("poiIds") List<Long> poiIds);

    @Query("select f.poi.id from POIFavorite f where f.poi.id in :poiIds and f.user.id = :userId")
    List<Long> findFavoritedPoiIdsByUserId(@Param("poiIds") List<Long> poiIds, @Param("userId") Long userId);
}
