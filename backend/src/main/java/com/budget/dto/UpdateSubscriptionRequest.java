package com.budget.dto;

import com.budget.model.RecurrenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSubscriptionRequest {
    private String name;

    @Positive
    private BigDecimal amount;

    @Min(1)
    @Max(31)
    private Integer billingDay;

    private String category;

    private RecurrenceType recurrence;
}
