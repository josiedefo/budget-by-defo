package com.budget.dto;

import com.budget.model.SavingsAccountEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class BulkLinkBudgetItemRequest {

    @NotNull
    private Long budgetItemId;

    @NotNull
    private SavingsAccountEventType eventType;

    private LocalDate startDate;
    private LocalDate endDate;

    private String note;
}
