package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.entity.POI;
import com.smartcampus.service.POIService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public Result<List<POI>> getAllPOIs() {
        return Result.success(poiService.getAllPOIs());
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
    public Result<List<POI>> searchByName(@RequestParam String name) {
        return Result.success(poiService.searchByName(name));
    }

    /**
     * 按分类查询POI
     * GET /api/pois/category/{category}
     */
    @GetMapping("/category/{category}")
    public Result<List<POI>> searchByCategory(@PathVariable String category) {
        return Result.success(poiService.searchByCategory(category));
    }

    /**
     * 按名称和分类组合查询
     * GET /api/pois/search/advanced?name=xxx&category=xxx
     */
    @GetMapping("/search/advanced")
    public Result<List<POI>> searchByNameAndCategory(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        if (name != null && category != null) {
            return Result.success(poiService.searchByNameAndCategory(name, category));
        } else if (name != null) {
            return Result.success(poiService.searchByName(name));
        } else if (category != null) {
            return Result.success(poiService.searchByCategory(category));
        } else {
            return Result.success(poiService.getAllPOIs());
        }
    }

    /**
     * 按坐标范围查询POI
     * GET /api/pois/bounds?minLat=xxx&maxLat=xxx&minLng=xxx&maxLng=xxx
     */
    @GetMapping("/bounds")
    public Result<List<POI>> findWithinBounds(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng) {
        return Result.success(poiService.findWithinBounds(minLat, maxLat, minLng, maxLng));
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
