package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIOptionResponse {

    private Long id;

    private String name;

    private String category;
}
