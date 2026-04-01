package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.RecommendedSharePublicResponse;
import com.smartcampus.service.RecommendedShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommended-shares")
@RequiredArgsConstructor
public class RecommendedShareController {

    private final RecommendedShareService recommendedShareService;

    @GetMapping
    public Result<List<RecommendedSharePublicResponse>> getRecommendedShares(
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(recommendedShareService.getPublicRecommendedShares(limit));
    }
}
