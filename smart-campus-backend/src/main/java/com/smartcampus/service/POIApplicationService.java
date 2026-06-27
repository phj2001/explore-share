package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.CreatePOIApplicationRequest;
import com.smartcampus.dto.request.ReviewPOIApplicationRequest;
import com.smartcampus.dto.response.AdminPOIApplicationListItemResponse;
import com.smartcampus.dto.response.POIApplicationResponse;

public interface POIApplicationService {

    POIApplicationResponse submitApplication(Long userId, CreatePOIApplicationRequest request);

    PageResponse<POIApplicationResponse> getMyApplications(Long userId, Integer page, Integer size);

    PageResponse<AdminPOIApplicationListItemResponse> getAdminList(String keyword, Short status, Integer page, Integer size);

    AdminPOIApplicationListItemResponse getAdminDetail(Long id);

    void reviewApplication(Long id, Long reviewerId, ReviewPOIApplicationRequest request);
}
