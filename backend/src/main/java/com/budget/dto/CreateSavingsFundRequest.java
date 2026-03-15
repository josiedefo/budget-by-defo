package com.budget.dto;

import com.budget.model.FundGoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSavingsFundRequest {
    @NotBlank
    private String name;

    @NotNull
    private FundGoalType goalType;

    @PositiveOrZero
    private BigDecimal targetAmount;

    private LocalDate deadline;

    @PositiveOrZero
    private BigDecimal ceiling;

    private LocalDate payoutDate;

    @PositiveOrZero
    private BigDecimal payoutAmount;
}
