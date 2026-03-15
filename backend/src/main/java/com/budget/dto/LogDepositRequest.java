package com.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LogDepositRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate eventDate;

    private String note;

    private Long targetFundId;
}
