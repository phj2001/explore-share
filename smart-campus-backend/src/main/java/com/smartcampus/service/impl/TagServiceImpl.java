package com.smartcampus.service.impl;

import com.smartcampus.dto.response.TagResponse;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareTag;
import com.smartcampus.entity.Tag;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.POIShareTagRepository;
import com.smartcampus.repository.TagRepository;
import com.smartcampus.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final POIShareTagRepository poiShareTagRepository;
    private final POIShareRepository poiShareRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getHotTags() {
        return tagRepository.findTop20ByOrderByUsageCountDesc().stream()
                .map(t -> new TagResponse(t.getId(), t.getName(), t.getUsageCount()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getTagsForShare(Long shareId) {
        return poiShareTagRepository.findTagsByShareId(shareId).stream()
                .map(t -> new TagResponse(t.getId(), t.getName(), t.getUsageCount()))
                .toList();
    }

    @Override
    @Transactional
    public void updateTagsForShare(Long shareId, Long userId, List<String> tagNames) {
        POIShare share = poiShareRepository.findById(shareId)
                .orElseThrow(() -> new BusinessException(404, "分享不存在"));
        if (!share.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "无权修改此分享的标签");
        }

        // 移除旧标签关联，减少 usage_count
        List<Tag> oldTags = poiShareTagRepository.findTagsByShareId(shareId);
        for (Tag old : oldTags) {
            tagRepository.updateUsageCount(old.getId(), -1);
        }
        poiShareTagRepository.deleteByShareId(shareId);

        if (tagNames == null || tagNames.isEmpty()) return;

        // 限制最多 5 个标签
        List<String> limited = tagNames.stream().limit(5).toList();
        List<Tag> newTags = new ArrayList<>();
        for (String name : limited) {
            String trimmed = name.trim().toLowerCase();
            if (trimmed.isEmpty() || trimmed.length() > 50) continue;
            Tag tag = tagRepository.findByName(trimmed).orElseGet(() -> {
                Tag newTag = new Tag();
                newTag.setName(trimmed);
                return tagRepository.save(newTag);
            });
            tagRepository.updateUsageCount(tag.getId(), 1);
            newTags.add(tag);
        }

        for (Tag tag : newTags) {
            POIShareTag pst = new POIShareTag();
            pst.setShare(share);
            pst.setTag(tag);
            poiShareTagRepository.save(pst);
        }
    }
}
