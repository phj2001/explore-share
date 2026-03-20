package com.smartcampus.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.config.AmapWebProperties;
import com.smartcampus.dto.response.RoutePlanResponse;
import com.smartcampus.dto.response.RoutePointResponse;
import com.smartcampus.dto.response.RouteStepResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.enums.RouteMode;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.service.RouteService;
import com.smartcampus.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");

    private final POIRepository poiRepository;
    private final AmapWebProperties amapWebProperties;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RoutePlanResponse planRoute(Double startLat,
                                       Double startLng,
                                       Double endLat,
                                       Double endLng,
                                       RouteMode mode) {
        validateCoordinates(startLat, startLng, endLat, endLng);
        ensureAmapKeyConfigured();

        URI uri = buildRouteRequestUri(startLat, startLng, endLat, endLng, mode);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException(502, "高德路径规划服务调用失败，HTTP 状态码: " + response.statusCode());
            }

            return parseRouteResponse(response.body(), startLat, startLng, endLat, endLng, mode);
        } catch (IOException e) {
            throw new BusinessException(502, "调用高德路径规划服务失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "路径规划请求被中断");
        }
    }

    @Override
    public List<POI> findNearbyPOIs(Double lat, Double lng, Double radiusMeters) {
        if (!GeoUtils.isValidCoordinates(lat, lng)) {
            throw new BusinessException(400, "查询附近 POI 的坐标不合法");
        }
        if (radiusMeters == null || radiusMeters <= 0) {
            throw new BusinessException(400, "查询附近 POI 的半径必须大于 0");
        }

        double radiusKm = radiusMeters / 1000.0;
        double[] bounds = GeoUtils.calculateBounds(lat, lng, radiusKm);
        return poiRepository.findWithinBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    private URI buildRouteRequestUri(Double startLat,
                                     Double startLng,
                                     Double endLat,
                                     Double endLng,
                                     RouteMode mode) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(amapWebProperties.getBaseUrl())
                .path(mode.getApiPath())
                .queryParam("origin", formatCoordinate(startLng, startLat))
                .queryParam("destination", formatCoordinate(endLng, endLat))
                .queryParam("output", "JSON")
                .queryParam("key", amapWebProperties.getKey());

        if (mode == RouteMode.DRIVING) {
            builder.queryParam("extensions", "all");
        }

        return builder.build(true).toUri();
    }

    private RoutePlanResponse parseRouteResponse(String body,
                                                 Double startLat,
                                                 Double startLng,
                                                 Double endLat,
                                                 Double endLng,
                                                 RouteMode mode) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (!mode.isSuccess(root)) {
            throw new BusinessException(502, "高德" + mode.getLabel() + "路径规划失败: " + mode.extractErrorMessage(root));
        }

        JsonNode firstPath = mode.extractFirstPath(root);
        if (firstPath.isMissingNode() || firstPath.isNull()) {
            throw new BusinessException(404, "未找到可用的" + mode.getLabel() + "路线");
        }

        int distanceMeters = firstPath.path("distance").asInt(0);
        int durationSeconds = firstPath.path("duration").asInt(0);
        List<RoutePointResponse> points = new ArrayList<>();
        List<RouteStepResponse> steps = new ArrayList<>();

        JsonNode stepsNode = extractStepsNode(firstPath);
        for (JsonNode stepNode : stepsNode) {
            List<RoutePointResponse> stepPoints = parsePolyline(stepNode.path("polyline").asText(""));
            appendUniquePoints(points, stepPoints);
            steps.add(new RouteStepResponse(
                    stepNode.path("instruction").asText(""),
                    stepNode.path("road").asText(""),
                    stepNode.path("distance").asInt(0),
                    stepNode.path("duration").asInt(0),
                    stepNode.path("action").asText(""),
                    stepNode.path("assistant_action").asText(""),
                    stepPoints
            ));
        }

        if (points.isEmpty()) {
            appendUniquePoints(points, parsePolyline(firstPath.path("polyline").asText("")));
        }

        return new RoutePlanResponse(
                mode.getValue(),
                mode.getLabel(),
                startLat,
                startLng,
                endLat,
                endLng,
                distanceMeters,
                durationSeconds,
                formatDistance(distanceMeters),
                formatDuration(durationSeconds),
                points,
                steps
        );
    }

    private JsonNode extractStepsNode(JsonNode firstPath) {
        JsonNode steps = firstPath.path("steps");
        if (steps.isArray() && !steps.isEmpty()) {
            return steps;
        }

        JsonNode rides = firstPath.path("rides");
        if (rides.isArray() && !rides.isEmpty()) {
            return rides;
        }

        return steps;
    }

    private void validateCoordinates(Double startLat, Double startLng, Double endLat, Double endLng) {
        if (!GeoUtils.isValidCoordinates(startLat, startLng)) {
            throw new BusinessException(400, "起点坐标不合法");
        }
        if (!GeoUtils.isValidCoordinates(endLat, endLng)) {
            throw new BusinessException(400, "终点坐标不合法");
        }
    }

    private void ensureAmapKeyConfigured() {
        if (!StringUtils.hasText(amapWebProperties.getKey())) {
            throw new BusinessException(500, "未配置高德 Web 服务 Key，请先在本地配置 AMAP_WEB_KEY");
        }
    }

    private String formatCoordinate(Double lng, Double lat) {
        return String.format("%.6f,%.6f", lng, lat);
    }

    private List<RoutePointResponse> parsePolyline(String polyline) {
        List<RoutePointResponse> points = new ArrayList<>();
        if (!StringUtils.hasText(polyline)) {
            return points;
        }

        String[] pairs = polyline.split(";");
        for (String pair : pairs) {
            String[] values = pair.split(",");
            if (values.length != 2) {
                continue;
            }

            double lng = Double.parseDouble(values[0]);
            double lat = Double.parseDouble(values[1]);
            points.add(new RoutePointResponse(lat, lng));
        }
        return points;
    }

    private void appendUniquePoints(List<RoutePointResponse> target, List<RoutePointResponse> source) {
        for (RoutePointResponse point : source) {
            if (target.isEmpty()) {
                target.add(point);
                continue;
            }

            RoutePointResponse last = target.get(target.size() - 1);
            if (!last.getLat().equals(point.getLat()) || !last.getLng().equals(point.getLng())) {
                target.add(point);
            }
        }
    }

    private String formatDistance(int distanceMeters) {
        if (distanceMeters >= 1000) {
            return DECIMAL_FORMAT.format(distanceMeters / 1000.0) + " 公里";
        }
        return distanceMeters + " 米";
    }

    private String formatDuration(int durationSeconds) {
        int totalMinutes = (int) Math.ceil(durationSeconds / 60.0);
        if (totalMinutes >= 60) {
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            return minutes == 0 ? hours + " 小时" : hours + " 小时 " + minutes + " 分钟";
        }
        return Math.max(totalMinutes, 1) + " 分钟";
    }
}
