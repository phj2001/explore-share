package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusResponse {

    private boolean following;

    private boolean follower;

    private long followingCount;

    private long followerCount;
}
