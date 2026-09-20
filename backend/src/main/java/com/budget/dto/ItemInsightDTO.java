package com.budget.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Year-long spending insight for a single budget item, matched across months by
 * (sectionName, itemName) case-insensitively — the same matching rule BudgetService.copyBudget
 * uses, since each month has its own BudgetItem rows with distinct IDs.
 */
@Data
@NoArgsConstructor
public class ItemInsightDTO {
    private Integer year;
    private String sectionName;
    private String itemName;
    private Boolean isIncome;
    private List<MonthInsightPoint> months;

    private BigDecimal annualPlanned;
    private BigDecimal annualActual;
    private BigDecimal ytdActual;
    private BigDecimal monthlyAverageActual;

    /** The month the headline "share of spending" figures below refer to. */
    private Integer currentMonth;
    private BigDecimal currentMonthActual;
    /** Item's actual amount divided by that month's total (0..1+, item actual / month total). */
    private BigDecimal currentMonthShare;

    @Data
    @NoArgsConstructor
    public static class MonthInsightPoint {
        private Integer month;
        private BigDecimal planned;
        private BigDecimal actual;
        /** That month's total actual expenses (or income, if this item is an income item). */
        private BigDecimal monthTotal;
    }
}
