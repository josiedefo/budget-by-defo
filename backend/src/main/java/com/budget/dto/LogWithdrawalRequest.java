package com.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LogWithdrawalRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private Long fundId;

    private String note;
}
