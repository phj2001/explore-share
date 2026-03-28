package com.smartcampus.repository;

import com.smartcampus.entity.AdminOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long>, JpaSpecificationExecutor<AdminOperationLog> {

    @Override
    Page<AdminOperationLog> findAll(Specification<AdminOperationLog> spec, Pageable pageable);
}
