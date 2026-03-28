package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.AdminRenamePoiCategoryRequest;
import com.smartcampus.dto.response.AdminPoiCategoryListItemResponse;
import com.smartcampus.service.AdminPoiCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/poi-categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPoiCategoryController {

    private final AdminPoiCategoryService adminPoiCategoryService;

    @GetMapping
    public Result<List<AdminPoiCategoryListItemResponse>> getCategories() {
        return Result.success(adminPoiCategoryService.getCategories());
    }

    @PutMapping("/{categoryName}")
    public Result<Void> renameCategory(
            @PathVariable String categoryName,
            @Valid @RequestBody AdminRenamePoiCategoryRequest request,
            Authentication authentication
    ) {
        adminPoiCategoryService.renameCategory(categoryName, request.getNewName(), getCurrentUserId(authentication));
        return Result.success();
    }

    @DeleteMapping("/{categoryName}")
    public Result<Void> deleteCategory(@PathVariable String categoryName, Authentication authentication) {
        adminPoiCategoryService.deleteCategory(categoryName, getCurrentUserId(authentication));
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
