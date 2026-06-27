package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.FeedItemResponse;

public interface FeedService {

    PageResponse<FeedItemResponse> getFeed(Long userId, Integer page, Integer size);
}
