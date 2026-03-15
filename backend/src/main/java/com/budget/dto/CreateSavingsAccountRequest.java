package com.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSavingsAccountRequest {
    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private BigDecimal balance;

    private LocalDate asOfDate;
}
