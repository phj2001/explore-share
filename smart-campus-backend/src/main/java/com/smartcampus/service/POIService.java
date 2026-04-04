package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIBoundsResponse;
import com.smartcampus.dto.response.POIImportResult;
import com.smartcampus.dto.response.POIMapPointResponse;
import com.smartcampus.dto.response.POIOptionResponse;
import com.smartcampus.dto.response.POIQueryResponse;
import com.smartcampus.dto.response.POIResponse;
import com.smartcampus.entity.POI;
import org.springframework.web.multipart.MultipartFile;

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
    POIQueryResponse getAllPOIs(Integer limit);

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

    POIQueryResponse getSearchResponse(String name, String category, Integer limit);

    /**
     * 按坐标范围查询
     */
    List<POI> findWithinBounds(Double minLat, Double maxLat, Double minLng, Double maxLng);

    List<POIMapPointResponse> findMapPointsWithinBounds(Double minLat, Double maxLat, Double minLng, Double maxLng, Integer limit);

    POIBoundsResponse getBoundsResponse(Double minLat, Double maxLat, Double minLng, Double maxLng, Integer limit);

    /**
     * 获取所有分类
     */
    List<String> getAllCategories();

    PageResponse<POIResponse> getPOIPage(String keyword, String category, Integer page, Integer size);

    long countAllPOIs();

    List<POIOptionResponse> searchOptions(String keyword, Integer limit);

    POIImportResult importFromCsv(MultipartFile file, boolean replaceExisting, boolean skipDuplicates);
}
