package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPoiCategoryListItemResponse {

    private String name;
    private Long poiCount;

    public static AdminPoiCategoryListItemResponse of(String name, Long poiCount) {
        return new AdminPoiCategoryListItemResponse(name, poiCount);
    }
}
