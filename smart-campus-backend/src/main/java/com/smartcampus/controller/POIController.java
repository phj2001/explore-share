package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.POIBoundsResponse;
import com.smartcampus.dto.response.POIImportResult;
import com.smartcampus.dto.response.POIMapPointResponse;
import com.smartcampus.dto.response.POIOptionResponse;
import com.smartcampus.dto.response.POIQueryResponse;
import com.smartcampus.dto.response.POIResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.service.POIService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pois")
@RequiredArgsConstructor
public class POIController {

    private final POIService poiService;

    /**
     * 创建POI
     * POST /api/pois
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<POI> createPOI(@RequestBody POI poi) {
        POI createdPOI = poiService.createPOI(poi);
        return Result.success(createdPOI);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<POIImportResult> importPOIs(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean replaceExisting,
            @RequestParam(defaultValue = "true") boolean skipDuplicates
    ) {
        return Result.success(poiService.importFromCsv(file, replaceExisting, skipDuplicates));
    }

    /**
     * 根据ID获取POI
     * GET /api/pois/{id}
     */
    @GetMapping("/{id}")
    public Result<POI> getPOIById(@PathVariable Long id) {
        return poiService.getPOIById(id)
                .map(Result::success)
                .orElse(Result.error(404, "POI不存在"));
    }

    /**
     * 获取所有POI
     * GET /api/pois
     */
    @GetMapping
    public Result<POIQueryResponse> getAllPOIs(
            @RequestParam(defaultValue = "300") Integer limit) {
        return Result.success(poiService.getAllPOIs(limit));
    }

    @GetMapping("/page")
    public Result<PageResponse<POIResponse>> getPOIPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(poiService.getPOIPage(keyword, category, page, size));
    }

    @GetMapping("/count")
    public Result<Long> countAllPOIs() {
        return Result.success(poiService.countAllPOIs());
    }

    @GetMapping("/options")
    public Result<List<POIOptionResponse>> getPOIOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(poiService.searchOptions(keyword, limit));
    }

    /**
     * 更新POI
     * PUT /api/pois/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<POI> updatePOI(@PathVariable Long id, @RequestBody POI poi) {
        poi.setId(id);
        POI updatedPOI = poiService.updatePOI(poi);
        return Result.success(updatedPOI);
    }

    /**
     * 删除POI
     * DELETE /api/pois/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> deletePOI(@PathVariable Long id) {
        poiService.deletePOI(id);
        return Result.success();
    }

    /**
     * 按名称搜索POI
     * GET /api/pois/search?name=xxx
     */
    @GetMapping("/search")
    public Result<POIQueryResponse> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "300") Integer limit) {
        return Result.success(poiService.getSearchResponse(name, null, limit));
    }

    /**
     * 按分类查询POI
     * GET /api/pois/category/{category}
     */
    @GetMapping("/category/{category}")
    public Result<POIQueryResponse> searchByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "300") Integer limit) {
        return Result.success(poiService.getSearchResponse(null, category, limit));
    }

    /**
     * 按名称和分类组合查询
     * GET /api/pois/search/advanced?name=xxx&category=xxx
     */
    @GetMapping("/search/advanced")
    public Result<POIQueryResponse> searchByNameAndCategory(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "300") Integer limit) {
        return Result.success(poiService.getSearchResponse(name, category, limit));
    }

    /**
     * 按坐标范围查询POI
     * GET /api/pois/bounds?minLat=xxx&maxLat=xxx&minLng=xxx&maxLng=xxx
     */
    @GetMapping("/bounds")
    public Result<POIBoundsResponse> findWithinBounds(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng,
            @RequestParam(defaultValue = "1200") Integer limit) {
        return Result.success(poiService.getBoundsResponse(minLat, maxLat, minLng, maxLng, limit));
    }

    /**
     * 获取所有分类
     * GET /api/pois/categories
     */
    @GetMapping("/categories")
    public Result<List<String>> getAllCategories() {
        return Result.success(poiService.getAllCategories());
    }
}
