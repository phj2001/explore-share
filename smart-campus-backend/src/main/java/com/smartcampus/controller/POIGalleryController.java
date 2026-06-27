package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.PoiGalleryImageResponse;
import com.smartcampus.entity.POIShareImage;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.POIShareImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class POIGalleryController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final POIShareImageRepository poiShareImageRepository;
    private final POIRepository poiRepository;

    @GetMapping("/api/pois/{poiId}/gallery")
    public Result<PageResponse<PoiGalleryImageResponse>> getGallery(
            @PathVariable Long poiId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        poiRepository.findById(poiId)
                .orElseThrow(() -> new BusinessException(404, "POI不存在"));

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIShareImage> result = poiShareImageRepository.findBySharePoiIdOrderByCreatedAtDesc(poiId, pageable);

        List<PoiGalleryImageResponse> records = result.getContent().stream()
                .map(img -> new PoiGalleryImageResponse(
                        img.getId(),
                        img.getImageUrl(),
                        img.getShare().getId(),
                        img.getShare().getUser().getId(),
                        img.getShare().getUser().getDisplayName(),
                        img.getShare().getUser().getAvatarUrl(),
                        img.getCreatedAt()
                ))
                .toList();

        return Result.success(new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext()));
    }
}
