package com.smartcampus.repository;

import com.smartcampus.entity.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, String> {

    List<AchievementDefinition> findAllByOrderBySortOrderAsc();
}
