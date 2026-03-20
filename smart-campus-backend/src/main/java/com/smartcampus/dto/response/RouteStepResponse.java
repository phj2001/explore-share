package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStepResponse {

    private String instruction;

    private String road;

    private Integer distanceMeters;

    private Integer durationSeconds;

    private String action;

    private String assistantAction;

    private List<RoutePointResponse> points = new ArrayList<>();
}
