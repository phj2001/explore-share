package com.smartcampus.util;

import java.math.BigDecimal;

/**
 * 地理位置工具类
 */
public class GeoUtils {

    /**
     * 验证坐标是否有效
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return true if coordinates are valid
     */
    public static boolean isValidCoordinates(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    /**
     * 验证坐标是否有效（BigDecimal版本）
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return true if coordinates are valid
     */
    public static boolean isValidCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return isValidCoordinates(latitude.doubleValue(), longitude.doubleValue());
    }

    /**
     * 根据中心点和半径计算边界
     *
     * @param centerLat 中心点纬度
     * @param centerLng 中心点经度
     * @param radius    半径（千米）
     * @return [minLat, maxLat, minLng, maxLng]
     */
    public static double[] calculateBounds(double centerLat, double centerLng, double radius) {
        double latDelta = radius / 111.0;
        double lngDelta = radius / (111.0 * Math.cos(Math.toRadians(centerLat)));

        return new double[]{
                centerLat - latDelta,
                centerLat + latDelta,
                centerLng - lngDelta,
                centerLng + lngDelta
        };
    }

    /**
     * Haversine 球面距离（米）。
     *
     * <p>用途（M2）：边界框 {@link #calculateBounds} 是半径圆的外接正方形，四角超出真实半径，
     * 需用它对候选 POI 做精确二次过滤，补齐 ST_DWithin 圆域精度（方案 §6.2，任务规则 4：
     * 边界框预过滤 + 精确距离过滤）。M3 {@code nearbyPois} 工具同样复用。
     *
     * @return 两点间大圆距离（米），地球平均半径取 6371000m
     */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusMeters = 6371000.0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return 2 * earthRadiusMeters * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
