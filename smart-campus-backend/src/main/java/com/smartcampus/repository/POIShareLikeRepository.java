package com.smartcampus.repository;

import com.smartcampus.entity.POIShareLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface POIShareLikeRepository extends JpaRepository<POIShareLike, Long> {

    Optional<POIShareLike> findByShareIdAndUserId(Long shareId, Long userId);

    boolean existsByShareIdAndUserId(Long shareId, Long userId);

    long countByShareId(Long shareId);

    void deleteByShareId(Long shareId);

    @Query("select l.share.id, count(l.id) from POIShareLike l where l.share.id in :shareIds group by l.share.id")
    List<Object[]> countGroupedByShareIds(@Param("shareIds") Collection<Long> shareIds);

    @Query("select l.share.id from POIShareLike l where l.share.id in :shareIds and l.user.id = :userId")
    List<Long> findLikedShareIdsByUserId(@Param("shareIds") Collection<Long> shareIds, @Param("userId") Long userId);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    long countByUserId(Long userId);
}
