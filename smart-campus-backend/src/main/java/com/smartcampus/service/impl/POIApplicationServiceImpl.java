package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.CreatePOIApplicationRequest;
import com.smartcampus.dto.request.ReviewPOIApplicationRequest;
import com.smartcampus.dto.response.AdminPOIApplicationListItemResponse;
import com.smartcampus.dto.response.POIApplicationResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.POIApplication;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIApplicationRepository;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.NotificationService;
import com.smartcampus.service.POIService;
import com.smartcampus.service.POIApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class POIApplicationServiceImpl implements POIApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final POIApplicationRepository poiApplicationRepository;
    private final POIRepository poiRepository;
    private final UserRepository userRepository;
    private final POIService poiService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public POIApplicationResponse submitApplication(Long userId, CreatePOIApplicationRequest request) {
        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (poiApplicationRepository.existsByApplicantIdAndNameAndStatus(userId, request.getName(), POIApplication.STATUS_PENDING)) {
            throw new BusinessException(400, "您已提交过同名地点的申请，请等待审核");
        }

        List<String> categories = poiService.getAllCategories();
        if (!categories.contains(request.getCategory())) {
            throw new BusinessException(400, "无效的分类");
        }

        POIApplication app = new POIApplication();
        app.setApplicant(applicant);
        app.setName(request.getName().trim());
        app.setCategory(request.getCategory().trim());
        app.setDescription(request.getDescription());
        app.setLatitude(request.getLatitude());
        app.setLongitude(request.getLongitude());
        app.setAddress(request.getAddress());

        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            List<String> limited = request.getPhotoUrls().stream().limit(3).toList();
            app.setPhotoUrls(String.join(",", limited));
        }

        POIApplication saved = poiApplicationRepository.save(app);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIApplicationResponse> getMyApplications(Long userId, Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIApplication> result = poiApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(userId, pageable);
        List<POIApplicationResponse> records = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminPOIApplicationListItemResponse> getAdminList(String keyword, Short status, Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIApplication> result;
        if (keyword == null && status == null) {
            result = poiApplicationRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else if (keyword == null) {
            result = poiApplicationRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            result = poiApplicationRepository.searchByKeywordAndStatus(keyword, status, pageable);
        }

        List<AdminPOIApplicationListItemResponse> records = result.getContent().stream().map(this::toAdminResponse).toList();
        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPOIApplicationListItemResponse getAdminDetail(Long id) {
        POIApplication app = poiApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "申请不存在"));
        return toAdminResponse(app);
    }

    @Override
    @Transactional
    public void reviewApplication(Long id, Long reviewerId, ReviewPOIApplicationRequest request) {
        POIApplication app = poiApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "申请不存在"));

        if (app.getStatus() != POIApplication.STATUS_PENDING) {
            throw new BusinessException(400, "该申请已审核，不可重复操作");
        }

        Short newStatus = request.getStatus();
        if (newStatus != POIApplication.STATUS_APPROVED && newStatus != POIApplication.STATUS_REJECTED) {
            throw new BusinessException(400, "无效的审核状态");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new BusinessException(404, "审核人不存在"));

        app.setStatus(newStatus);
        app.setReviewedBy(reviewer);
        app.setReviewedAt(java.time.LocalDateTime.now());
        app.setReviewNote(request.getReviewNote());

        if (newStatus == POIApplication.STATUS_APPROVED) {
            POI poi = new POI();
            poi.setName(app.getName());
            poi.setCategory(app.getCategory());
            poi.setDescription(app.getDescription());
            poi.setLatitude(app.getLatitude());
            poi.setLongitude(app.getLongitude());
            LocalDateTime now = LocalDateTime.now();
            poi.setCreatedAt(now);
            poi.setUpdatedAt(now);
            POI savedPoi = poiRepository.save(poi);
            app.setCreatedPoiId(savedPoi.getId());

            notificationService.sendNotification(
                    app.getApplicant().getId(), reviewerId, "POI_APPROVED",
                    "您申请的地点「" + app.getName() + "」已通过审核", null,
                    "POI_APPLICATION", app.getId());
        } else {
            notificationService.sendNotification(
                    app.getApplicant().getId(), reviewerId, "POI_REJECTED",
                    "您申请的地点「" + app.getName() + "」未通过审核",
                    request.getReviewNote(), "POI_APPLICATION", app.getId());
        }

        poiApplicationRepository.save(app);
    }

    private POIApplicationResponse toResponse(POIApplication app) {
        return new POIApplicationResponse(
                app.getId(), app.getName(), app.getCategory(), app.getDescription(),
                app.getLatitude(), app.getLongitude(), app.getAddress(), app.getPhotoUrls(),
                app.getStatus(), app.getReviewNote(), app.getCreatedPoiId(),
                app.getCreatedAt(), app.getReviewedAt());
    }

    private AdminPOIApplicationListItemResponse toAdminResponse(POIApplication app) {
        User applicant = app.getApplicant();
        User reviewer = app.getReviewedBy();
        return new AdminPOIApplicationListItemResponse(
                app.getId(),
                applicant.getId(),
                applicant.getDisplayName() != null ? applicant.getDisplayName() : applicant.getUsername(),
                applicant.getAvatarUrl(),
                app.getName(), app.getCategory(), app.getLatitude(), app.getLongitude(),
                app.getAddress(), app.getDescription(), app.getStatus(),
                reviewer != null ? (reviewer.getDisplayName() != null ? reviewer.getDisplayName() : reviewer.getUsername()) : null,
                app.getReviewNote(), app.getCreatedPoiId(), app.getCreatedAt(), app.getReviewedAt());
    }
}
