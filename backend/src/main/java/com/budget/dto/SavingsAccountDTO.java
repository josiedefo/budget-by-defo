package com.budget.dto;

import com.budget.model.SavingsAccount;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SavingsAccountDTO {
    private Long id;
    private String name;
    private BigDecimal balance;
    private LocalDate asOfDate;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static SavingsAccountDTO fromEntity(SavingsAccount account) {
        SavingsAccountDTO dto = new SavingsAccountDTO();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setBalance(account.getBalance());
        dto.setAsOfDate(account.getAsOfDate());
        dto.setIsActive(account.getIsActive());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }
}
