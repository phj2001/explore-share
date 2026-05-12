package com.smartcampus.service;

import com.smartcampus.dto.response.HotPoiResponse;
import com.smartcampus.dto.response.LeaderboardItemResponse;

import java.util.List;

public interface LeaderboardService {

    List<LeaderboardItemResponse> getLeaderboard(String type, String period, int limit);

    List<HotPoiResponse> getHotPois(int limit);
}
