package com.smartcampus.service;

public interface ContentReportService {

    void createShareReport(Long shareId, Long reporterId, Short reasonCode, String reasonDetail);

    void createReplyReport(Long replyId, Long reporterId, Short reasonCode, String reasonDetail);
}
