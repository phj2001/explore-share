package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotPoiResponse {

    private Long poiId;

    private String poiName;

    private String category;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private long checkInCount;
}
