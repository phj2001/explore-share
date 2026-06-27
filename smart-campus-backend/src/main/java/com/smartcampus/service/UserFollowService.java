package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.FollowStatusResponse;
import com.smartcampus.dto.response.FollowUserItemResponse;

import java.util.List;

public interface UserFollowService {

    void follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    FollowStatusResponse getFollowStatus(Long currentUserId, Long targetUserId);

    PageResponse<FollowUserItemResponse> getFollowingList(Long userId, Integer page, Integer size);

    PageResponse<FollowUserItemResponse> getFollowerList(Long userId, Integer page, Integer size);

    List<Long> getFollowingIds(Long userId);
}
