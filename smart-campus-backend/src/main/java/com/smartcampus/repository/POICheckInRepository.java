package com.smartcampus.repository;

import com.smartcampus.entity.POICheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface POICheckInRepository extends JpaRepository<POICheckIn, Long> {

    boolean existsByPoiIdAndUserId(Long poiId, Long userId);

    Optional<POICheckIn> findByPoiIdAndUserId(Long poiId, Long userId);

    long countByPoiId(Long poiId);
}
