package com.budget.dto;

import com.budget.model.BudgetItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BudgetItemDTO {
    private Long id;
    private String name;
    private BigDecimal plannedAmount;
    private BigDecimal actualAmount;
    private Integer displayOrder;
    private BigDecimal difference;
    private Boolean isExcludedFromBudget;
    private Long planId;

    public static BudgetItemDTO fromEntity(BudgetItem item) {
        BudgetItemDTO dto = new BudgetItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setPlannedAmount(item.getPlannedAmount());
        dto.setActualAmount(item.getActualAmount());
        dto.setDisplayOrder(item.getDisplayOrder());
        dto.setDifference(item.getPlannedAmount().subtract(item.getActualAmount()));
        dto.setIsExcludedFromBudget(item.getIsExcludedFromBudget());
        return dto;
    }
}
