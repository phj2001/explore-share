package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminReplyListItemResponse;
import com.smartcampus.dto.response.AdminShareDetailResponse;
import com.smartcampus.dto.response.AdminShareListItemResponse;

import java.time.LocalDateTime;

public interface AdminContentService {

    PageResponse<AdminShareListItemResponse> getShares(
            String keyword,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer page,
            Integer size
    );

    AdminShareDetailResponse getShareDetail(Long shareId);

    void deleteShare(Long shareId, Long operatorUserId);

    PageResponse<AdminReplyListItemResponse> getReplies(
            String keyword,
            Long shareId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer page,
            Integer size
    );

    void deleteReply(Long replyId, Long operatorUserId);
}
