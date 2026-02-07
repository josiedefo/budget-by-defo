package com.budget.dto;

import com.budget.model.Plan;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class PlanDTO {
    private Long id;
    private Long budgetItemId;
    private String budgetItemName;
    private String sectionName;
    private Integer year;
    private Integer month;
    private LocalDateTime createdAt;
    private List<PlanItemDTO> items;
    private BigDecimal total;

    public static PlanDTO fromEntity(Plan plan) {
        PlanDTO dto = new PlanDTO();
        dto.setId(plan.getId());
        dto.setBudgetItemId(plan.getBudgetItem().getId());
        dto.setBudgetItemName(plan.getBudgetItem().getName());
        dto.setSectionName(plan.getBudgetItem().getSection().getName());
        dto.setYear(plan.getYear());
        dto.setMonth(plan.getMonth());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setItems(plan.getItems().stream()
                .map(PlanItemDTO::fromEntity)
                .collect(Collectors.toList()));
        dto.setTotal(dto.getItems().stream()
                .map(PlanItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return dto;
    }
}
