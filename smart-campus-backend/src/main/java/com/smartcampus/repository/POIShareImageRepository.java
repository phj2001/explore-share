package com.smartcampus.repository;

import com.smartcampus.entity.POIShareImage;
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
}
