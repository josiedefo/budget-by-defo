package com.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSalaryRequest {
    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private BigDecimal regularAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal federalTax;

    @NotNull
    @PositiveOrZero
    private BigDecimal medicare;

    @NotNull
    @PositiveOrZero
    private BigDecimal socialSecurity;

    @PositiveOrZero
    private BigDecimal fourOhOneK;

    @PositiveOrZero
    private BigDecimal extraTaxWithholding;

    @PositiveOrZero
    private BigDecimal healthSavingsAccount;

    @PositiveOrZero
    private BigDecimal medicalInsurance;

    @PositiveOrZero
    private BigDecimal flexibleSpendingAccount;
}
