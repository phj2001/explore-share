package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminContentReportDetailResponse;
import com.smartcampus.dto.response.AdminContentReportListItemResponse;

public interface AdminContentReportService {

    PageResponse<AdminContentReportListItemResponse> getReports(
            String keyword,
            Short targetType,
            Short status,
            Short reasonCode,
            Integer page,
            Integer size
    );

    AdminContentReportDetailResponse getReportDetail(Long reportId);

    AdminContentReportDetailResponse reviewReport(Long reportId, Short status, Short action, String reviewNote, Long operatorUserId);
}
