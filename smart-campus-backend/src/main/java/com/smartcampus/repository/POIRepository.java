package com.smartcampus.repository;

import com.smartcampus.entity.POI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface POIRepository extends JpaRepository<POI, Long> {

    /**
     * 按名称模糊搜索POI
     */
    List<POI> findByNameContainingIgnoreCase(String name);

    /**
     * 按分类查询POI
     */
    List<POI> findByCategory(String category);

    /**
     * 按名称和分类查询（支持模糊搜索）
     */
    List<POI> findByNameContainingIgnoreCaseAndCategory(String name, String category);

    /**
     * 按坐标范围查询POI（矩形区域）
     */
    @Query("SELECT p FROM POI p WHERE p.latitude BETWEEN :minLat AND :maxLat " +
           "AND p.longitude BETWEEN :minLng AND :maxLng")
    List<POI> findWithinBounds(@Param("minLat") Double minLat,
                               @Param("maxLat") Double maxLat,
                               @Param("minLng") Double minLng,
                               @Param("maxLng") Double maxLng);

    /**
     * 按分类查询所有不重复的分类列表
     */
    @Query("SELECT DISTINCT p.category FROM POI p")
    List<String> findAllCategories();
}
