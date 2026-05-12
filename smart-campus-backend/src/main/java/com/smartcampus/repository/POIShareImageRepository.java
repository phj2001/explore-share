package com.smartcampus.repository;

import com.smartcampus.entity.POIShareImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface POIShareImageRepository extends JpaRepository<POIShareImage, Long> {

    @EntityGraph(attributePaths = {"share", "share.user", "share.poi"})
    List<POIShareImage> findByImageUrl(String imageUrl);

    @Override
    @EntityGraph(attributePaths = {"share", "share.user", "share.poi"})
    List<POIShareImage> findAll();

    @EntityGraph(attributePaths = {"share", "share.user"})
    Page<POIShareImage> findBySharePoiIdOrderByCreatedAtDesc(Long poiId, Pageable pageable);
}
