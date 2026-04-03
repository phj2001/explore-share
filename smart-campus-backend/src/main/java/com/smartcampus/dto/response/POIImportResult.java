package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIImportResult {

    private String fileName;

    private Integer totalRows;

    private Integer importedCount;

    private Integer skippedCount;

    private Integer duplicateCount;

    private Integer invalidCount;

    private Integer emptyRowCount;

    private Integer clearedCount;

    private Boolean replaceExisting;

    private Boolean skipDuplicates;

    private List<POIImportRowError> errors;
}
