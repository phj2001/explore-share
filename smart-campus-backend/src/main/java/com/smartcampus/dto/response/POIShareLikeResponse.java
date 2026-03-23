package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIShareLikeResponse {

    private Long shareId;
    private Long likeCount;
    private Boolean likedByCurrentUser;
}
