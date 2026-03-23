package com.smartcampus.repository;

import com.smartcampus.entity.POIShareReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface POIShareReplyRepository extends JpaRepository<POIShareReply, Long> {

    @EntityGraph(attributePaths = {"user", "share"})
    Page<POIShareReply> findByShareIdOrderByCreatedAtAscIdAsc(Long shareId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "share"})
    Optional<POIShareReply> findWithUserAndShareById(Long id);

    @EntityGraph(attributePaths = {"user", "share"})
    List<POIShareReply> findTop3ByShareIdOrderByCreatedAtAscIdAsc(Long shareId);

    long countByShareId(Long shareId);

    void deleteByShareId(Long shareId);

    @Query("select r.share.id, count(r.id) from POIShareReply r where r.share.id in :shareIds group by r.share.id")
    List<Object[]> countGroupedByShareIds(@Param("shareIds") Collection<Long> shareIds);
}
