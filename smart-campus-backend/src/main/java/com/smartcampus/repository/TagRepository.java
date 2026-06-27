package com.smartcampus.repository;

import com.smartcampus.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findTop20ByOrderByUsageCountDesc();

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount + :delta WHERE t.id = :id")
    void updateUsageCount(@Param("id") Long id, @Param("delta") int delta);
}
