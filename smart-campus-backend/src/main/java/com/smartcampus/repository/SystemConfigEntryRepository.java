package com.smartcampus.repository;

import com.smartcampus.entity.SystemConfigEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigEntryRepository extends JpaRepository<SystemConfigEntry, String> {
}
