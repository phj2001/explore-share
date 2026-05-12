package com.smartcampus.service;

import com.smartcampus.dto.response.TagResponse;

import java.util.List;

public interface TagService {

    List<TagResponse> getHotTags();

    List<TagResponse> getTagsForShare(Long shareId);

    void updateTagsForShare(Long shareId, Long userId, List<String> tagNames);
}
