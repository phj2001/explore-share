package com.smartcampus.service;

import com.smartcampus.entity.POI;

import java.util.List;
import java.util.Optional;

public interface POIService {

    /**
     * 创建POI
     */
    POI createPOI(POI poi);

    /**
     * 根据ID获取POI
     */
    Optional<POI> getPOIById(Long id);

    /**
     * 获取所有POI
     */
    List<POI> getAllPOIs();

    /**
     * 更新POI
     */
    POI updatePOI(POI poi);

    /**
     * 删除POI
     */
    void deletePOI(Long id);

    /**
     * 按名称模糊搜索
     */
    List<POI> searchByName(String name);

    /**
     * 按分类查询
     */
    List<POI> searchByCategory(String category);

    /**
     * 按名称和分类组合查询
     */
    List<POI> searchByNameAndCategory(String name, String category);

    /**
     * 按坐标范围查询
     */
    List<POI> findWithinBounds(Double minLat, Double maxLat, Double minLng, Double maxLng);

    /**
     * 获取所有分类
     */
    List<String> getAllCategories();
}
