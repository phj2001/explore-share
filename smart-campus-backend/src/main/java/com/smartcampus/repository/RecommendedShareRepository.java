package com.smartcampus.repository;

import com.smartcampus.entity.RecommendedShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendedShareRepository extends JpaRepository<RecommendedShare, Long>, JpaSpecificationExecutor<RecommendedShare> {

    @Override
    @EntityGraph(attributePaths = {"share", "share.poi", "share.user", "share.images"})
    Page<RecommendedShare> findAll(Specification<RecommendedShare> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"share", "share.poi", "share.user", "share.images"})
    Optional<RecommendedShare> findWithShareById(Long id);

    @EntityGraph(attributePaths = {"share", "share.poi", "share.user", "share.images"})
    List<RecommendedShare> findAllByOrderBySortOrderAscIdDesc(Pageable pageable);

    boolean existsByShareId(Long shareId);

    Optional<RecommendedShare> findByShareId(Long shareId);

    void deleteByShareId(Long shareId);
}
