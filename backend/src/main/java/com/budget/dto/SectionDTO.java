package com.budget.dto;

import com.budget.model.Section;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class SectionDTO {
    private Long id;
    private String name;
    private Integer displayOrder;
    private Boolean isIncome;
    private List<BudgetItemDTO> items;
    private BigDecimal totalPlanned;
    private BigDecimal totalActual;

    public static SectionDTO fromEntity(Section section) {
        SectionDTO dto = new SectionDTO();
        dto.setId(section.getId());
        dto.setName(section.getName());
        dto.setDisplayOrder(section.getDisplayOrder());
        dto.setIsIncome(section.getIsIncome());
        dto.setItems(section.getItems().stream()
                .map(BudgetItemDTO::fromEntity)
                .collect(Collectors.toList()));

        dto.setTotalPlanned(dto.getItems().stream()
                .map(BudgetItemDTO::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        dto.setTotalActual(dto.getItems().stream()
                .map(BudgetItemDTO::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return dto;
    }
}
