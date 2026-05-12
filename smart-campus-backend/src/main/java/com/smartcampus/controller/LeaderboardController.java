package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.HotPoiResponse;
import com.smartcampus.dto.response.LeaderboardItemResponse;
import com.smartcampus.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/api/leaderboard")
    public Result<List<LeaderboardItemResponse>> getLeaderboard(
            @RequestParam(defaultValue = "checkin") String type,
            @RequestParam(defaultValue = "total") String period,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(leaderboardService.getLeaderboard(type, period, limit));
    }

    @GetMapping("/api/leaderboard/hot-pois")
    public Result<List<HotPoiResponse>> getHotPois(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(leaderboardService.getHotPois(limit));
    }
}
