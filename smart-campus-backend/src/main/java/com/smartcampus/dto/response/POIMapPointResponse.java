package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIMapPointResponse {

    private Long id;
    private String name;
    private String category;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
