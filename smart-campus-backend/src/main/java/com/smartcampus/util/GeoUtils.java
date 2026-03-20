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
}
