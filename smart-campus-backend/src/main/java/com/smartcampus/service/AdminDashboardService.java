package com.smartcampus.service;

import com.smartcampus.dto.response.AdminOverviewResponse;

public interface AdminDashboardService {

    AdminOverviewResponse getOverview(Integer days);
}
