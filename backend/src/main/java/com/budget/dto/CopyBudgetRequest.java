package com.budget.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CopyBudgetRequest {

    @NotNull
    @Min(2000) @Max(2100)
    private Integer sourceYear;

    @NotNull
    @Min(1) @Max(12)
    private Integer sourceMonth;
}
