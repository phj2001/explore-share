package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminFileResourceListItemResponse {

    private String resourceType;
    private String resourceUrl;
    private String filename;
    private Long fileSize;
    private LocalDateTime lastModifiedAt;
    private String ownerType;
    private Long ownerId;
    private String ownerName;
    private Boolean referenced;
    private Boolean fileExists;
    private String status;
}
