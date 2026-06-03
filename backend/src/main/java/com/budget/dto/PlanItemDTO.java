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
    private Boolean fromSubscription;
    private Boolean fromSalary;
    private Long subscriptionId;
    private Long salaryId;

    public static PlanItemDTO fromEntity(PlanItem item) {
        PlanItemDTO dto = new PlanItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setAmount(item.getAmount());
        dto.setDisplayOrder(item.getDisplayOrder());
        dto.setFromSubscription(item.getFromSubscription());
        dto.setFromSalary(item.getFromSalary());
        dto.setSubscriptionId(item.getSubscription() != null ? item.getSubscription().getId() : null);
        dto.setSalaryId(item.getSalary() != null ? item.getSalary().getId() : null);
        return dto;
    }
}
