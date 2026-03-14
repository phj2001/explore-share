package com.smartcampus.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 地理位置工具类
 */
public class GeoUtils {

    /**
     * 地球半径（单位：千米）
     */
    private static final double EARTH_RADIUS = 6371.0;

    /**
     * 计算两点之间的距离（单位：千米）
     * 使用Haversine公式
     *
     * @param lat1 第一个点的纬度
     * @param lng1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lng2 第二个点的经度
     * @return 两点之间的距离（千米）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // 将角度转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double lng1Rad = Math.toRadians(lng1);
        double lng2Rad = Math.toRadians(lng2);

        // Haversine公式
        double dLat = lat2Rad - lat1Rad;
        double dLng = lng2Rad - lng1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * 计算两点之间的距离（BigDecimal版本）
     *
     * @param lat1 第一个点的纬度
     * @param lng1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lng2 第二个点的经度
     * @return 两点之间的距离（千米），保留2位小数
     */
    public static BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                               BigDecimal lat2, BigDecimal lng2) {
        double distance = calculateDistance(
                lat1.doubleValue(), lng1.doubleValue(),
                lat2.doubleValue(), lng2.doubleValue()
        );
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

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
        // 粗略计算：1度纬度约111千米，1度经度约111*cos(纬度)千米
        double latDelta = radius / 111.0;
        double lngDelta = radius / (111.0 * Math.cos(Math.toRadians(centerLat)));

        return new double[]{
                centerLat - latDelta, // minLat
                centerLat + latDelta, // maxLat
                centerLng - lngDelta, // minLng
                centerLng + lngDelta  // maxLng
        };
    }
}
