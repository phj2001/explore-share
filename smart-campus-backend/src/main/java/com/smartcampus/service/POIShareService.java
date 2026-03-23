package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIShareLikeResponse;
import com.smartcampus.dto.response.POIShareReplyResponse;
import com.smartcampus.dto.response.POIShareResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface POIShareService {

    PageResponse<POIShareResponse> getSharesByPoi(Long poiId, Integer page, Integer size, Long currentUserId);

    POIShareResponse createShare(Long poiId, Long userId, String content, List<MultipartFile> images);

    void deleteShare(Long shareId, Long userId);

    POIShareLikeResponse likeShare(Long shareId, Long userId);

    POIShareLikeResponse unlikeShare(Long shareId, Long userId);

    PageResponse<POIShareReplyResponse> getRepliesByShare(Long shareId, Integer page, Integer size, Long currentUserId);

    POIShareReplyResponse createReply(Long shareId, Long userId, String content);

    void deleteReply(Long replyId, Long userId);
}
