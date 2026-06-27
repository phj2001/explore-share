package com.smartcampus.repository;

import com.smartcampus.entity.POIApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface POIApplicationRepository extends JpaRepository<POIApplication, Long> {

    Page<POIApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);

    Page<POIApplication> findByStatusOrderByCreatedAtDesc(Short status, Pageable pageable);

    Page<POIApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(Short status);

    long countByApplicantId(Long applicantId);

    boolean existsByApplicantIdAndNameAndStatus(Long applicantId, String name, Short status);

    long countByApplicantIdAndStatus(Long applicantId, Short status);

    @Query("SELECT a FROM POIApplication a WHERE " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:keyword IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(a.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<POIApplication> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") Short status,
            Pageable pageable);
}
