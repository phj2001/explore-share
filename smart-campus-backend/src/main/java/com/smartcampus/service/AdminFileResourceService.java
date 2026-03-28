package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminFileResourceListItemResponse;

public interface AdminFileResourceService {

    PageResponse<AdminFileResourceListItemResponse> getResources(String keyword, String resourceType, String status, Integer page, Integer size);

    void deleteResource(String resourceType, String resourceUrl, Long ownerId, Long operatorUserId);
}
