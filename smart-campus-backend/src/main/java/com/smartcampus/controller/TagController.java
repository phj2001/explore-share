package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.TagResponse;
import com.smartcampus.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/api/tags/hot")
    public Result<List<TagResponse>> getHotTags() {
        return Result.success(tagService.getHotTags());
    }

    @GetMapping("/api/poi-shares/{shareId}/tags")
    public Result<List<TagResponse>> getShareTags(@PathVariable Long shareId) {
        return Result.success(tagService.getTagsForShare(shareId));
    }

    @PutMapping("/api/poi-shares/{shareId}/tags")
    public Result<Void> updateShareTags(
            @PathVariable Long shareId,
            @RequestBody Map<String, List<String>> body,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("未登录或登录已失效");
        }
        tagService.updateTagsForShare(shareId, userId, body.getOrDefault("tags", List.of()));
        return Result.success(null);
    }
}
