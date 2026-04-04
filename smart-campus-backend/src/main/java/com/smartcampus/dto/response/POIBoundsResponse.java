package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIBoundsResponse {

    private List<POIMapPointResponse> records;
    private Long total;
    private Integer limit;
    private Boolean truncated;
}
