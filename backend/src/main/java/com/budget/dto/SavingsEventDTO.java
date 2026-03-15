package com.budget.dto;

import com.budget.model.SavingsEvent;
import com.budget.model.SavingsEventType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SavingsEventDTO {
    private Long id;
    private Long fundId;
    private String fundName;
    private SavingsEventType eventType;
    private BigDecimal amount;
    private LocalDate eventDate;
    private String note;
    private Long transactionRef;
    private LocalDateTime createdAt;

    public static SavingsEventDTO fromEntity(SavingsEvent event) {
        SavingsEventDTO dto = new SavingsEventDTO();
        dto.setId(event.getId());
        dto.setFundId(event.getFund().getId());
        dto.setFundName(event.getFund().getName());
        dto.setEventType(event.getEventType());
        dto.setAmount(event.getAmount());
        dto.setEventDate(event.getEventDate());
        dto.setNote(event.getNote());
        dto.setTransactionRef(event.getTransactionRef());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}
