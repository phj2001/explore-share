package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIFavoriteResponse;
import com.smartcampus.dto.response.POIFavoriteStatusResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.POIFavorite;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIFavoriteRepository;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.POIFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class POIFavoriteServiceImpl implements POIFavoriteService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final POIFavoriteRepository poiFavoriteRepository;
    private final POIRepository poiRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public POIFavoriteStatusResponse getStatus(Long poiId, Long userId) {
        getRequiredPoi(poiId);
        long favoriteCount = poiFavoriteRepository.countByPoiId(poiId);
        boolean favorited = userId != null && poiFavoriteRepository.existsByPoiIdAndUserId(poiId, userId);
        return new POIFavoriteStatusResponse(favorited, favoriteCount);
    }

    @Override
    @Transactional
    public POIFavoriteStatusResponse addFavorite(Long poiId, Long userId) {
        POI poi = getRequiredPoi(poiId);
        User user = getRequiredUser(userId);

        if (!poiFavoriteRepository.existsByPoiIdAndUserId(poi.getId(), user.getId())) {
            POIFavorite favorite = new POIFavorite();
            favorite.setPoi(poi);
            favorite.setUser(user);
            poiFavoriteRepository.save(favorite);
        }

        return buildStatus(poi.getId(), user.getId(), true);
    }

    @Override
    @Transactional
    public POIFavoriteStatusResponse removeFavorite(Long poiId, Long userId) {
        getRequiredPoi(poiId);
        getRequiredUser(userId);

        poiFavoriteRepository.deleteByPoiIdAndUserId(poiId, userId);

        return buildStatus(poiId, userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIFavoriteResponse> getUserFavorites(Long userId, Integer page, Integer size) {
        getRequiredUser(userId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIFavorite> result = poiFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        java.util.List<POIFavoriteResponse> records = result.getContent().stream()
                .map(this::toFavoriteResponse)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    private POIFavoriteStatusResponse buildStatus(Long poiId, Long userId, boolean favorited) {
        long favoriteCount = poiFavoriteRepository.countByPoiId(poiId);
        return new POIFavoriteStatusResponse(favorited, favoriteCount);
    }

    private POIFavoriteResponse toFavoriteResponse(POIFavorite favorite) {
        POI poi = favorite.getPoi();
        return new POIFavoriteResponse(
                favorite.getId(),
                poi.getId(),
                poi.getName(),
                poi.getCategory(),
                poi.getDescription(),
                poi.getLatitude(),
                poi.getLongitude(),
                favorite.getCreatedAt()
        );
    }

    private POI getRequiredPoi(Long poiId) {
        return poiRepository.findById(poiId)
                .orElseThrow(() -> new BusinessException(404, "POI不存在"));
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
