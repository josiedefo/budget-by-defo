package com.budget.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class YearlySummaryDTO {
    private Integer year;
    private List<MonthSummaryDTO> months;
    private BigDecimal totalPlannedIncome;
    private BigDecimal totalActualIncome;
    private BigDecimal totalPlannedExpenses;
    private BigDecimal totalActualExpenses;
    private BigDecimal totalPlannedSavings;
    private BigDecimal totalActualSavings;

    @Data
    @NoArgsConstructor
    public static class MonthSummaryDTO {
        private Integer month;
        private Long budgetId;
        private BigDecimal plannedIncome;
        private BigDecimal actualIncome;
        private BigDecimal plannedExpenses;
        private BigDecimal actualExpenses;
        private BigDecimal plannedSavings;
        private BigDecimal actualSavings;
    }
}
