package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.UserCheckInItemResponse;
import com.smartcampus.dto.response.UserPublicProfileResponse;
import com.smartcampus.dto.response.POIShareResponse;

public interface UserPublicProfileService {

    UserPublicProfileResponse getPublicProfile(Long userId);

    PageResponse<POIShareResponse> getUserPublicShares(Long userId, Integer page, Integer size);

    PageResponse<UserCheckInItemResponse> getUserCheckIns(Long userId, Integer page, Integer size);
}
