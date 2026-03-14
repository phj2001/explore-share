package com.smartcampus.service;

import java.util.List;

public interface RouteService {

    /**
     * 规划两点之间的路径
     * @param startLat 起点纬度
     * @param startLng 起点经度
     * @param endLat 终点纬度
     * @param endLng 终点经度
     * @return 路径上的POI点列表
     */
    List<Object> planRoute(Double startLat, Double startLng, Double endLat, Double endLng);

    /**
     * 查询附近的POI
     * @param lat 中心点纬度
     * @param lng 中心点经度
     * @param radius 半径（单位：公里）
     * @return 附近的POI列表
     */
    List<Object> findNearbyPOIs(Double lat, Double lng, Double radius);
}
