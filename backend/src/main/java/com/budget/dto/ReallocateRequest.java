package com.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReallocateRequest {
    @NotNull
    private Long sourceFundId;

    @NotNull
    private Long destinationFundId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String note;
}
