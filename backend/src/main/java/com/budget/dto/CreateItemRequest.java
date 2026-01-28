package com.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateItemRequest {
    @NotNull
    private Long sectionId;

    @NotBlank
    private String name;

    private BigDecimal plannedAmount = BigDecimal.ZERO;

    private BigDecimal actualAmount = BigDecimal.ZERO;
}
