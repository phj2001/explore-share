package com.smartcampus.service;

import com.smartcampus.dto.response.HotPoiResponse;
import com.smartcampus.dto.response.LeaderboardItemResponse;

import java.util.List;

public interface LeaderboardService {

    List<LeaderboardItemResponse> getLeaderboard(String type, String period, int limit);

    List<HotPoiResponse> getHotPois(int limit);

    /** 清除全部用户维度榜单缓存（3 类型 × 总榜/周榜）。隐私档位或账号状态变更后调用，避免最长 1h 缓存期内继续展示已隐藏用户。 */
    void evictUserLeaderboards();
}
