package com.smartcampus.repository;

import com.smartcampus.entity.Announcement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {

    List<Announcement> findByStatusOrderByPinnedDescPublishedAtDescIdDesc(Short status, Pageable pageable);

    Optional<Announcement> findByIdAndStatus(Long id, Short status);
}
