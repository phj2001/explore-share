package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.UserCheckInItemResponse;
import com.smartcampus.dto.response.UserPublicProfileResponse;
import com.smartcampus.dto.response.POIShareResponse;

public interface UserPublicProfileService {

    /** viewerId 为查看者（游客为 null）；受限时返回精简响应（contentVisible=false） */
    UserPublicProfileResponse getPublicProfile(Long userId, Long viewerId);

    PageResponse<POIShareResponse> getUserPublicShares(Long userId, Integer page, Integer size, Long viewerId);

    /** viewerId 为查看者（游客为 null）；不可见时抛 403 */
    PageResponse<UserCheckInItemResponse> getUserCheckIns(Long userId, Integer page, Integer size, Long viewerId);
}
