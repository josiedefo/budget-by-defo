package com.budget.dto;

import com.budget.model.FundGoalType;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateSavingsFundRequest {
    private String name;
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
