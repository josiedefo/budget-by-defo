package com.budget.dto;

import com.budget.model.SavingsAccountEvent;
import com.budget.model.SavingsAccountEventType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SavingsAccountEventDTO {
    private Long id;
    private Long accountId;
    private String accountName;
    private SavingsAccountEventType eventType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDate eventDate;
    private String note;
    private LocalDateTime createdAt;

    public static SavingsAccountEventDTO fromEntity(SavingsAccountEvent event) {
        SavingsAccountEventDTO dto = new SavingsAccountEventDTO();
        dto.setId(event.getId());
        dto.setAccountId(event.getAccount().getId());
        dto.setAccountName(event.getAccount().getName());
        dto.setEventType(event.getEventType());
        dto.setAmount(event.getAmount());
        dto.setBalanceAfter(event.getBalanceAfter());
        dto.setEventDate(event.getEventDate());
        dto.setNote(event.getNote());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}
