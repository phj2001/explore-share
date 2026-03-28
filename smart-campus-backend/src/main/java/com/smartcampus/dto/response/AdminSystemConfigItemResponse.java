package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemConfigItemResponse {

    private String configKey;
    private String label;
    private String description;
    private String valueType;
    private String value;
    private String defaultValue;
    private Boolean publicVisible;
}
