package com.smartcampus.repository;

import com.smartcampus.entity.POIShareImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface POIShareImageRepository extends JpaRepository<POIShareImage, Long> {
}
