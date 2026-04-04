package com.smartcampus.dto.response;

import java.util.List;

public record POIQueryResponse(
        List<POIMapPointResponse> records,
        long total,
        int limit,
        boolean truncated
) {
}
