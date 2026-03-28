package com.smartcampus.repository;

import com.smartcampus.entity.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, Long>, JpaSpecificationExecutor<ContentReport> {

    @Override
    @EntityGraph(attributePaths = {"reporter", "reviewedBy"})
    Page<ContentReport> findAll(Specification<ContentReport> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "reviewedBy"})
    Optional<ContentReport> findById(Long id);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, Short targetType, Long targetId, Short status);
}
