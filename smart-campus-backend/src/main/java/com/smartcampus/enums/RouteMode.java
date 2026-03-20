package com.smartcampus.enums;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;

public enum RouteMode {

    WALKING("walking", "步行", "/v3/direction/walking", "route", "status", "1"),
    DRIVING("driving", "驾车", "/v3/direction/driving", "route", "status", "1"),
    BICYCLING("bicycling", "骑行", "/v4/direction/bicycling", "data", "errcode", "0");

    private final String value;
    private final String label;
    private final String apiPath;
    private final String rootField;
    private final String successField;
    private final String successValue;

    RouteMode(String value,
              String label,
              String apiPath,
              String rootField,
              String successField,
              String successValue) {
        this.value = value;
        this.label = label;
        this.apiPath = apiPath;
        this.rootField = rootField;
        this.successField = successField;
        this.successValue = successValue;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getApiPath() {
        return apiPath;
    }

    public JsonNode extractRootNode(JsonNode response) {
        return response.path(rootField);
    }

    public JsonNode extractFirstPath(JsonNode response) {
        return extractRootNode(response).path("paths").path(0);
    }

    public boolean isSuccess(JsonNode response) {
        return successValue.equals(response.path(successField).asText());
    }

    public String extractErrorMessage(JsonNode response) {
        String info = response.path("info").asText("");
        if (!info.isBlank()) {
            return info;
        }
        String errMsg = response.path("errmsg").asText("");
        if (!errMsg.isBlank()) {
            return errMsg;
        }
        return "未知错误";
    }

    public static RouteMode fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return WALKING;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "walking", "walk" -> WALKING;
            case "driving", "drive", "car" -> DRIVING;
            case "bicycling", "bicycle", "bike", "cycling" -> BICYCLING;
            default -> throw new IllegalArgumentException("不支持的导航模式: " + rawValue);
        };
    }
}
