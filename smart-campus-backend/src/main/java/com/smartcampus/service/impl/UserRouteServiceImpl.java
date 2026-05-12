package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.CreateUserRouteRequest;
import com.smartcampus.dto.response.UserRouteListItemResponse;
import com.smartcampus.dto.response.UserRouteResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.User;
import com.smartcampus.entity.UserRoute;
import com.smartcampus.entity.UserRouteFavorite;
import com.smartcampus.entity.UserRouteLike;
import com.smartcampus.entity.UserRouteWaypoint;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.repository.UserRouteFavoriteRepository;
import com.smartcampus.repository.UserRouteLikeRepository;
import com.smartcampus.repository.UserRouteRepository;
import com.smartcampus.service.AchievementService;
import com.smartcampus.service.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRouteServiceImpl implements UserRouteService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final short STATUS_PUBLISHED = 1;

    private final UserRouteRepository userRouteRepository;
    private final UserRouteLikeRepository userRouteLikeRepository;
    private final UserRouteFavoriteRepository userRouteFavoriteRepository;
    private final UserRepository userRepository;
    private final POIRepository poiRepository;
    private final AchievementService achievementService;

    @Override
    @Transactional
    public UserRouteResponse createRoute(Long userId, CreateUserRouteRequest request) {
        User user = getRequiredUser(userId);
        UserRoute route = buildRoute(user, request);
        addWaypoints(route, request.getWaypoints());
        userRouteRepository.save(route);
        try { achievementService.checkAndUnlock(userId); } catch (Exception ignored) {}
        return toDetailResponse(route, userId);
    }

    @Override
    @Transactional
    public UserRouteResponse updateRoute(Long routeId, Long userId, CreateUserRouteRequest request) {
        UserRoute route = getOwnedRoute(routeId, userId);
        route.setTitle(request.getTitle());
        route.setSummary(request.getSummary());
        route.setDescription(request.getDescription());
        route.setDefaultMode(request.getDefaultMode() != null ? request.getDefaultMode() : "walking");
        route.setCoverImageUrl(request.getCoverImageUrl());

        route.getWaypoints().clear();
        addWaypoints(route, request.getWaypoints());

        userRouteRepository.save(route);
        return toDetailResponse(route, userId);
    }

    @Override
    @Transactional
    public void deleteRoute(Long routeId, Long userId) {
        UserRoute route = getOwnedRoute(routeId, userId);
        userRouteRepository.delete(route);
    }

    @Override
    @Transactional(readOnly = true)
    public UserRouteResponse getRouteDetail(Long routeId, Long currentUserId) {
        UserRoute route = userRouteRepository.findWithDetailsById(routeId);
        if (route == null) {
            throw new BusinessException(404, "路线不存在");
        }
        return toDetailResponse(route, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserRouteListItemResponse> getPublicRoutes(Integer page, Integer size) {
        Pageable pageable = buildPageable(page, size);
        Page<UserRoute> result = userRouteRepository.findByStatusOrderByCreatedAtDesc(STATUS_PUBLISHED, pageable);
        List<UserRouteListItemResponse> records = result.getContent().stream()
                .map(this::toListResponse)
                .toList();
        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserRouteListItemResponse> getMyRoutes(Long userId, Integer page, Integer size) {
        getRequiredUser(userId);
        Pageable pageable = buildPageable(page, size);
        Page<UserRoute> result = userRouteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<UserRouteListItemResponse> records = result.getContent().stream()
                .map(this::toListResponse)
                .toList();
        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserRouteListItemResponse> getMyFavoriteRoutes(Long userId, Integer page, Integer size) {
        getRequiredUser(userId);
        Pageable pageable = buildPageable(page, size);
        Page<UserRouteFavorite> result = userRouteFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<UserRouteListItemResponse> records = result.getContent().stream()
                .map(fav -> toListResponse(fav.getRoute()))
                .toList();
        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional
    public boolean toggleLike(Long routeId, Long userId) {
        getRequiredUser(userId);
        UserRoute route = userRouteRepository.findById(routeId)
                .orElseThrow(() -> new BusinessException(404, "路线不存在"));

        if (userRouteLikeRepository.existsByRouteIdAndUserId(routeId, userId)) {
            userRouteLikeRepository.deleteByRouteIdAndUserId(routeId, userId);
            route.setLikeCount(Math.max(0, route.getLikeCount() - 1));
            return false;
        } else {
            UserRouteLike like = new UserRouteLike();
            like.setRoute(route);
            like.setUser(getRequiredUser(userId));
            userRouteLikeRepository.save(like);
            route.setLikeCount(route.getLikeCount() + 1);
            return true;
        }
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long routeId, Long userId) {
        getRequiredUser(userId);
        UserRoute route = userRouteRepository.findById(routeId)
                .orElseThrow(() -> new BusinessException(404, "路线不存在"));

        if (userRouteFavoriteRepository.existsByRouteIdAndUserId(routeId, userId)) {
            userRouteFavoriteRepository.deleteByRouteIdAndUserId(routeId, userId);
            route.setFavoriteCount(Math.max(0, route.getFavoriteCount() - 1));
            return false;
        } else {
            UserRouteFavorite fav = new UserRouteFavorite();
            fav.setRoute(route);
            fav.setUser(getRequiredUser(userId));
            userRouteFavoriteRepository.save(fav);
            route.setFavoriteCount(route.getFavoriteCount() + 1);
            return true;
        }
    }

    private UserRoute buildRoute(User user, CreateUserRouteRequest request) {
        UserRoute route = new UserRoute();
        route.setUser(user);
        route.setTitle(request.getTitle());
        route.setSummary(request.getSummary());
        route.setDescription(request.getDescription());
        route.setDefaultMode(request.getDefaultMode() != null ? request.getDefaultMode() : "walking");
        route.setCoverImageUrl(request.getCoverImageUrl());
        route.setStatus(STATUS_PUBLISHED);
        return route;
    }

    private void addWaypoints(UserRoute route, List<CreateUserRouteRequest.WaypointInput> inputs) {
        if (inputs == null) return;
        for (int i = 0; i < inputs.size(); i++) {
            CreateUserRouteRequest.WaypointInput input = inputs.get(i);
            UserRouteWaypoint wp = new UserRouteWaypoint();
            wp.setRoute(route);
            if (input.getPoiId() != null) {
                POI poi = poiRepository.findById(input.getPoiId()).orElse(null);
                wp.setPoi(poi);
                if (input.getWaypointName() == null && poi != null) {
                    wp.setWaypointName(poi.getName());
                }
                if (input.getLatitude() == null && poi != null) {
                    wp.setLatitude(poi.getLatitude());
                }
                if (input.getLongitude() == null && poi != null) {
                    wp.setLongitude(poi.getLongitude());
                }
            }
            if (input.getLatitude() != null) wp.setLatitude(input.getLatitude());
            if (input.getLongitude() != null) wp.setLongitude(input.getLongitude());
            if (input.getWaypointName() != null) wp.setWaypointName(input.getWaypointName());
            if (wp.getLatitude() == null) wp.setLatitude(java.math.BigDecimal.ZERO);
            if (wp.getLongitude() == null) wp.setLongitude(java.math.BigDecimal.ZERO);
            wp.setSortOrder(input.getSortOrder() != null ? input.getSortOrder() : i);
            route.getWaypoints().add(wp);
        }
    }

    private UserRouteResponse toDetailResponse(UserRoute route, Long currentUserId) {
        User user = route.getUser();
        boolean liked = currentUserId != null && userRouteLikeRepository.existsByRouteIdAndUserId(route.getId(), currentUserId);
        boolean favorited = currentUserId != null && userRouteFavoriteRepository.existsByRouteIdAndUserId(route.getId(), currentUserId);

        List<UserRouteResponse.WaypointResponse> waypoints = route.getWaypoints().stream()
                .map(wp -> new UserRouteResponse.WaypointResponse(
                        wp.getId(),
                        wp.getPoi() != null ? wp.getPoi().getId() : null,
                        wp.getPoi() != null ? wp.getPoi().getName() : wp.getWaypointName(),
                        wp.getLatitude(),
                        wp.getLongitude(),
                        wp.getWaypointName(),
                        wp.getSortOrder()
                ))
                .toList();

        return new UserRouteResponse(
                route.getId(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                route.getTitle(),
                route.getSummary(),
                route.getDescription(),
                route.getDefaultMode(),
                route.getCoverImageUrl(),
                route.getLikeCount(),
                route.getFavoriteCount(),
                liked,
                favorited,
                route.getCreatedAt(),
                route.getUpdatedAt(),
                waypoints
        );
    }

    private UserRouteListItemResponse toListResponse(UserRoute route) {
        User user = route.getUser();
        return new UserRouteListItemResponse(
                route.getId(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                route.getTitle(),
                route.getSummary(),
                route.getDefaultMode(),
                route.getCoverImageUrl(),
                route.getLikeCount(),
                route.getFavoriteCount(),
                route.getWaypoints() != null ? route.getWaypoints().size() : 0,
                route.getCreatedAt()
        );
    }

    private UserRoute getOwnedRoute(Long routeId, Long userId) {
        UserRoute route = userRouteRepository.findById(routeId)
                .orElseThrow(() -> new BusinessException(404, "路线不存在"));
        if (!route.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "无权操作此路线");
        }
        return route;
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
