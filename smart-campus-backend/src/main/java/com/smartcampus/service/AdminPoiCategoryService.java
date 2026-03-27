package com.smartcampus.service;

import com.smartcampus.dto.response.AdminPoiCategoryListItemResponse;

import java.util.List;

public interface AdminPoiCategoryService {

    List<AdminPoiCategoryListItemResponse> getCategories();

    void renameCategory(String oldName, String newName);

    void deleteCategory(String categoryName);
}
