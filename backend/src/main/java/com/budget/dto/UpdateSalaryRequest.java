package com.budget.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSalaryRequest {
    private String name;

    @PositiveOrZero
    private BigDecimal regularAmount;

    @PositiveOrZero
    private BigDecimal federalTax;

    @PositiveOrZero
    private BigDecimal medicare;

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
