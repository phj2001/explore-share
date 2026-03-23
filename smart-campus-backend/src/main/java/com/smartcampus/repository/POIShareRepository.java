package com.smartcampus.repository;

import com.smartcampus.entity.POIShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface POIShareRepository extends JpaRepository<POIShare, Long> {

    @EntityGraph(attributePaths = {"user", "images"})
    Page<POIShare> findByPoiId(Long poiId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    Optional<POIShare> findWithUserAndImagesById(Long id);
}
