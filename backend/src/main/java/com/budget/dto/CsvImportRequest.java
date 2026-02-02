package com.budget.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CsvImportRequest {
    @NotNull(message = "Column mapping is required")
    private Map<String, Integer> columnMapping; // field name -> column index

    @NotEmpty(message = "CSV rows are required")
    private List<List<String>> rows;

    private String dateFormat; // e.g., "yyyy-MM-dd", "MM/dd/yyyy"

    private boolean skipFirstRow; // true if first row is header
}
