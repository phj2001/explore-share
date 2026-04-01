package com.smartcampus.repository;

import com.smartcampus.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    List<Activity> findByStatus(Short status);

    Optional<Activity> findByIdAndStatus(Long id, Short status);

    List<Activity> findByCoverImageUrl(String coverImageUrl);
}
