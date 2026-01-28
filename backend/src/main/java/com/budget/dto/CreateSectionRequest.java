package com.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSectionRequest {
    @NotNull
    private Long budgetId;

    @NotBlank
    private String name;

    private Boolean isIncome = false;
}
