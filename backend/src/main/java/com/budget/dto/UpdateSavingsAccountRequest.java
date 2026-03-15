package com.budget.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateSavingsAccountRequest {
    private String name;

    @PositiveOrZero
    private BigDecimal balance;

    private LocalDate asOfDate;
}
