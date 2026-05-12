package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardItemResponse {

    private int rank;

    private Long userId;

    private String displayName;

    private String avatarUrl;

    private long count;
}
