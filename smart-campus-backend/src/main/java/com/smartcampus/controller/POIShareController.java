package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.CreatePOIShareReplyRequest;
import com.smartcampus.dto.response.POIShareLikeResponse;
import com.smartcampus.dto.response.POIShareReplyResponse;
import com.smartcampus.dto.response.POIShareResponse;
import com.smartcampus.service.POIShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/poi-shares")
@RequiredArgsConstructor
public class POIShareController {

    private final POIShareService poiShareService;

    @GetMapping("/poi/{poiId}")
    public Result<PageResponse<POIShareResponse>> getSharesByPoi(
            @PathVariable Long poiId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication
    ) {
        return Result.success(poiShareService.getSharesByPoi(poiId, page, size, getOptionalUserId(authentication)));
    }

    @PostMapping(value = "/poi/{poiId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<POIShareResponse> createShare(
            @PathVariable Long poiId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) {
        POIShareResponse response = poiShareService.createShare(poiId, getRequiredUserId(authentication), content, images);
        return Result.success(response);
    }

    @DeleteMapping("/{shareId}")
    public Result<Void> deleteShare(@PathVariable Long shareId, Authentication authentication) {
        poiShareService.deleteShare(shareId, getRequiredUserId(authentication));
        return Result.success();
    }

    @PostMapping("/{shareId}/likes")
    public Result<POIShareLikeResponse> likeShare(@PathVariable Long shareId, Authentication authentication) {
        return Result.success(poiShareService.likeShare(shareId, getRequiredUserId(authentication)));
    }

    @DeleteMapping("/{shareId}/likes")
    public Result<POIShareLikeResponse> unlikeShare(@PathVariable Long shareId, Authentication authentication) {
        return Result.success(poiShareService.unlikeShare(shareId, getRequiredUserId(authentication)));
    }

    @GetMapping("/{shareId}/replies")
    public Result<PageResponse<POIShareReplyResponse>> getRepliesByShare(
            @PathVariable Long shareId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "3") Integer size,
            Authentication authentication
    ) {
        return Result.success(poiShareService.getRepliesByShare(shareId, page, size, getOptionalUserId(authentication)));
    }

    @PostMapping("/{shareId}/replies")
    public Result<POIShareReplyResponse> createReply(
            @PathVariable Long shareId,
            @Valid @RequestBody CreatePOIShareReplyRequest request,
            Authentication authentication
    ) {
        return Result.success(poiShareService.createReply(shareId, getRequiredUserId(authentication), request.getContent()));
    }

    @DeleteMapping("/replies/{replyId}")
    public Result<Void> deleteReply(@PathVariable Long replyId, Authentication authentication) {
        poiShareService.deleteReply(replyId, getRequiredUserId(authentication));
        return Result.success();
    }

    private Long getOptionalUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    private Long getRequiredUserId(Authentication authentication) {
        Long userId = getOptionalUserId(authentication);
        if (userId == null) {
            throw new IllegalArgumentException("未登录或登录已失效");
        }
        return userId;
    }
}
