package com.budget.dto;

import com.budget.model.SavingsEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class BulkLinkBudgetItemToFundRequest {

    @NotNull
    private Long budgetItemId;

    @NotNull
    private Long fundId;

    @NotNull
    private SavingsEventType eventType;

    private LocalDate startDate;
    private LocalDate endDate;

    private String note;
}
