package com.smartcampus.service.impl;

import com.smartcampus.entity.ContentReport;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareReply;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.ContentReportRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.ContentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContentReportServiceImpl implements ContentReportService {

    private static final int MAX_DETAIL_LENGTH = 200;
    private static final int MAX_PREVIEW_LENGTH = 300;

    private final ContentReportRepository contentReportRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createShareReport(Long shareId, Long reporterId, Short reasonCode, String reasonDetail) {
        POIShare share = poiShareRepository.findWithPoiUserAndImagesById(shareId)
                .orElseThrow(() -> new BusinessException(404, "分享不存在"));
        User reporter = getRequiredUser(reporterId);
        validateTargetAuthor(share.getUser().getId(), reporterId);
        validateDuplicatePendingReport(reporterId, ContentReport.TARGET_TYPE_SHARE, shareId);

        ContentReport report = new ContentReport();
        report.setTargetType(ContentReport.TARGET_TYPE_SHARE);
        report.setTargetId(share.getId());
        report.setRelatedShareId(share.getId());
        report.setReporter(reporter);
        report.setReasonCode(normalizeReasonCode(reasonCode));
        report.setReasonDetail(normalizeReasonDetail(reasonDetail, reasonCode));
        report.setTargetContentPreview(buildPreview(share.getContent(), "该分享未填写文字，仅包含图片"));
        report.setTargetAuthorUserId(share.getUser().getId());
        report.setTargetAuthorDisplayName(resolveDisplayName(share.getUser()));
        report.setTargetAuthorUsername(share.getUser().getUsername());
        report.setTargetPoiName(share.getPoi().getName());
        contentReportRepository.save(report);
    }

    @Override
    @Transactional
    public void createReplyReport(Long replyId, Long reporterId, Short reasonCode, String reasonDetail) {
        POIShareReply reply = poiShareReplyRepository.findWithUserAndShareById(replyId)
                .orElseThrow(() -> new BusinessException(404, "回复不存在"));
        User reporter = getRequiredUser(reporterId);
        validateTargetAuthor(reply.getUser().getId(), reporterId);
        validateDuplicatePendingReport(reporterId, ContentReport.TARGET_TYPE_REPLY, replyId);

        ContentReport report = new ContentReport();
        report.setTargetType(ContentReport.TARGET_TYPE_REPLY);
        report.setTargetId(reply.getId());
        report.setRelatedShareId(reply.getShare().getId());
        report.setReporter(reporter);
        report.setReasonCode(normalizeReasonCode(reasonCode));
        report.setReasonDetail(normalizeReasonDetail(reasonDetail, reasonCode));
        report.setTargetContentPreview(buildPreview(reply.getContent(), "该回复内容为空"));
        report.setTargetAuthorUserId(reply.getUser().getId());
        report.setTargetAuthorDisplayName(resolveDisplayName(reply.getUser()));
        report.setTargetAuthorUsername(reply.getUser().getUsername());
        report.setTargetPoiName(reply.getShare().getPoi().getName());
        contentReportRepository.save(report);
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private void validateTargetAuthor(Long targetAuthorId, Long reporterId) {
        if (targetAuthorId != null && targetAuthorId.equals(reporterId)) {
            throw new BusinessException(400, "不能举报自己发布的内容");
        }
    }

    private void validateDuplicatePendingReport(Long reporterId, Short targetType, Long targetId) {
        if (contentReportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId,
                targetType,
                targetId,
                ContentReport.STATUS_PENDING
        )) {
            throw new BusinessException(400, "你已经举报过这条内容，等待管理员处理即可");
        }
    }

    private Short normalizeReasonCode(Short reasonCode) {
        if (reasonCode == null) {
            throw new BusinessException(400, "举报理由不能为空");
        }
        if (reasonCode < ContentReport.REASON_SPAM || reasonCode > ContentReport.REASON_OTHER) {
            throw new BusinessException(400, "不支持的举报理由");
        }
        return reasonCode;
    }

    private String normalizeReasonDetail(String reasonDetail, Short reasonCode) {
        String normalized = StringUtils.hasText(reasonDetail) ? reasonDetail.trim() : null;
        if (normalized != null && normalized.length() > MAX_DETAIL_LENGTH) {
            throw new BusinessException(400, "补充说明不能超过200个字符");
        }
        if (reasonCode != null && reasonCode == ContentReport.REASON_OTHER && !StringUtils.hasText(normalized)) {
            throw new BusinessException(400, "选择“其他”时请填写补充说明");
        }
        return normalized;
    }

    private String buildPreview(String content, String fallback) {
        String normalized = StringUtils.hasText(content) ? content.trim() : fallback;
        if (normalized.length() <= MAX_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_PREVIEW_LENGTH - 1) + "…";
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "未知用户";
        }
        return StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername();
    }
}
