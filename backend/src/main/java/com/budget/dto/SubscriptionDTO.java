package com.budget.dto;

import com.budget.model.RecurrenceType;
import com.budget.model.Subscription;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SubscriptionDTO {
    private Long id;
    private String name;
    private BigDecimal amount;
    private Integer billingDay;
    private String category;
    private RecurrenceType recurrence;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static SubscriptionDTO fromEntity(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setName(subscription.getName());
        dto.setAmount(subscription.getAmount());
        dto.setBillingDay(subscription.getBillingDay());
        dto.setCategory(subscription.getCategory());
        dto.setRecurrence(subscription.getRecurrence());
        dto.setIsActive(subscription.getIsActive());
        dto.setCreatedAt(subscription.getCreatedAt());
        return dto;
    }
}
