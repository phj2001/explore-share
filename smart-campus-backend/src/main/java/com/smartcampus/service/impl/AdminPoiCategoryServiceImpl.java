package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AdminPoiCategoryListItemResponse;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.AdminPoiCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPoiCategoryServiceImpl implements AdminPoiCategoryService {

    private static final int MAX_CATEGORY_LENGTH = 50;

    private final POIRepository poiRepository;
    private final AdminOperationLogService adminOperationLogService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminPoiCategoryListItemResponse> getCategories() {
        return poiRepository.countGroupedByCategory().stream()
                .map(row -> AdminPoiCategoryListItemResponse.of((String) row[0], (Long) row[1]))
                .toList();
    }

    @Override
    @Transactional
    public void renameCategory(String oldName, String newName, Long operatorUserId) {
        String normalizedOldName = normalizeCategoryName(oldName, "原分类名称不能为空");
        String normalizedNewName = normalizeCategoryName(newName, "新分类名称不能为空");

        if (normalizedOldName.equals(normalizedNewName)) {
            throw new BusinessException(400, "新旧分类名称不能相同");
        }

        long oldCategoryCount = poiRepository.countByCategory(normalizedOldName);
        if (oldCategoryCount == 0) {
            throw new BusinessException(404, "原分类不存在");
        }

        poiRepository.renameCategory(normalizedOldName, normalizedNewName, LocalDateTime.now());
        adminOperationLogService.record(
                operatorUserId,
                "POI分类",
                "重命名分类",
                "分类",
                null,
                "将分类“" + normalizedOldName + "”重命名为“" + normalizedNewName + "”"
        );
    }

    @Override
    @Transactional
    public void deleteCategory(String categoryName, Long operatorUserId) {
        String normalizedCategoryName = normalizeCategoryName(categoryName, "分类名称不能为空");
        long poiCount = poiRepository.countByCategory(normalizedCategoryName);

        if (poiCount > 0) {
            throw new BusinessException(400, "该分类下仍有关联 POI，请先调整或迁移后再删除");
        }

        throw new BusinessException(404, "分类不存在");
    }

    private String normalizeCategoryName(String categoryName, String emptyMessage) {
        if (!StringUtils.hasText(categoryName)) {
            throw new BusinessException(400, emptyMessage);
        }

        String normalized = categoryName.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, emptyMessage);
        }

        if (normalized.length() > MAX_CATEGORY_LENGTH) {
            throw new BusinessException(400, "分类名称不能超过50个字符");
        }

        return normalized;
    }
}
