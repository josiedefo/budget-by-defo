package com.budget.dto;

import com.budget.model.Budget;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class BudgetDTO {
    private Long id;
    private Integer year;
    private Integer month;
    private LocalDateTime createdAt;
    private List<SectionDTO> sections;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal totalPlannedIncome;
    private BigDecimal totalPlannedExpenses;

    public static BudgetDTO fromEntity(Budget budget) {
        BudgetDTO dto = new BudgetDTO();
        dto.setId(budget.getId());
        dto.setYear(budget.getYear());
        dto.setMonth(budget.getMonth());
        dto.setCreatedAt(budget.getCreatedAt());
        dto.setSections(budget.getSections().stream()
                .map(SectionDTO::fromEntity)
                .collect(Collectors.toList()));

        dto.setTotalPlannedIncome(dto.getSections().stream()
                .filter(SectionDTO::getIsIncome)
                .flatMap(s -> s.getItems().stream())
                .map(BudgetItemDTO::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        dto.setTotalIncome(dto.getSections().stream()
                .filter(SectionDTO::getIsIncome)
                .flatMap(s -> s.getItems().stream())
                .map(BudgetItemDTO::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        dto.setTotalPlannedExpenses(dto.getSections().stream()
                .filter(s -> !s.getIsIncome())
                .flatMap(s -> s.getItems().stream())
                .map(BudgetItemDTO::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        dto.setTotalExpenses(dto.getSections().stream()
                .filter(s -> !s.getIsIncome())
                .flatMap(s -> s.getItems().stream())
                .map(BudgetItemDTO::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return dto;
    }
}
