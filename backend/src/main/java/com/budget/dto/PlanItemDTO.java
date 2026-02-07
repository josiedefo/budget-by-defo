package com.budget.dto;

import com.budget.model.PlanItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PlanItemDTO {
    private Long id;
    private String name;
    private BigDecimal amount;
    private Integer displayOrder;

    public static PlanItemDTO fromEntity(PlanItem item) {
        PlanItemDTO dto = new PlanItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setAmount(item.getAmount());
        dto.setDisplayOrder(item.getDisplayOrder());
        return dto;
    }
}
