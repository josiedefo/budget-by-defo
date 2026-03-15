package com.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AccountWithdrawalRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate eventDate;

    private String note;
}
