package com.smartcampus.repository;

import com.smartcampus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查邮箱是否已存在
     */
    boolean existsByEmail(String email);

    boolean existsByRole(Short role);

    long countByCreatedAtGreaterThanEqual(java.time.LocalDateTime createdAt);

    List<User> findByAvatarUrl(String avatarUrl);
}
