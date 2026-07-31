package com.smartcampus.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link GeoUtils} 单元测试：坐标校验、边界框计算、Haversine 球面距离。
 * 这些是 AI 空间检索（bbox 预过滤 → Haversine 精确过滤）的数学基础，纯计算无外部依赖。
 */
class GeoUtilsTest {

    @Test
    void 有效坐标边界值() {
        assertThat(GeoUtils.isValidCoordinates(90, 180)).isTrue();
        assertThat(GeoUtils.isValidCoordinates(-90, -180)).isTrue();
        assertThat(GeoUtils.isValidCoordinates(0, 0)).isTrue();
    }

    @Test
    void 超出范围无效() {
        assertThat(GeoUtils.isValidCoordinates(90.1, 0)).isFalse();
        assertThat(GeoUtils.isValidCoordinates(0, 180.1)).isFalse();
        assertThat(GeoUtils.isValidCoordinates(-90.1, 0)).isFalse();
        assertThat(GeoUtils.isValidCoordinates(0, -180.1)).isFalse();
    }

    @Test
    void BigDecimal版本null返回false() {
        assertThat(GeoUtils.isValidCoordinates(null, BigDecimal.ZERO)).isFalse();
        assertThat(GeoUtils.isValidCoordinates(BigDecimal.ZERO, null)).isFalse();
    }

    @Test
    void calculateBounds对称且跨度正确() {
        // 北京 3km：1 纬度 ≈ 111km，故 3km ≈ 0.027 度
        double[] bounds = GeoUtils.calculateBounds(39.9, 116.4, 3.0);
        // [minLat, maxLat, minLng, maxLng]
        assertThat(bounds[0]).isLessThan(39.9);
        assertThat(bounds[1]).isGreaterThan(39.9);
        assertThat(bounds[2]).isLessThan(116.4);
        assertThat(bounds[3]).isGreaterThan(116.4);
        // 纬度跨度 = 2 * radius/111
        assertThat(bounds[1] - bounds[0]).isCloseTo(2 * 3.0 / 111.0, within(0.001));
    }

    @Test
    void haversine同点距离为零() {
        assertThat(GeoUtils.haversineMeters(39.9, 116.4, 39.9, 116.4)).isCloseTo(0.0, within(0.1));
    }

    @Test
    void haversine北京到上海约1067km() {
        // 天安门(39.908,116.397) → 外滩(31.24,121.47)，大圆距离约 1067km
        double d = GeoUtils.haversineMeters(39.908, 116.397, 31.24, 121.47);
        assertThat(d).isCloseTo(1_067_000.0, within(20_000.0)); // ±20km 容差（地球半径取平均值的近似）
    }

    @Test
    void haversine近距离约111米每001度() {
        // 纬度差 0.001 度 ≈ 111 米
        double d = GeoUtils.haversineMeters(39.9, 116.4, 39.901, 116.4);
        assertThat(d).isCloseTo(111.0, within(2.0));
    }
}
