package com.smartcampus.repository;

import com.smartcampus.entity.POIShareTag;
import com.smartcampus.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface POIShareTagRepository extends JpaRepository<POIShareTag, Long> {

    List<POIShareTag> findByShareId(Long shareId);

    @Modifying
    void deleteByShareId(Long shareId);

    @Query("SELECT pst.tag FROM POIShareTag pst WHERE pst.share.id = :shareId")
    List<Tag> findTagsByShareId(@Param("shareId") Long shareId);

    @Query("SELECT pst.share.id FROM POIShareTag pst WHERE pst.tag.id = :tagId")
    List<Long> findShareIdsByTagId(@Param("tagId") Long tagId);
}
