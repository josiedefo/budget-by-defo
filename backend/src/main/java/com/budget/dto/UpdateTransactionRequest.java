package com.budget.dto;

import com.budget.model.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateTransactionRequest {
    private Long sectionId;
    private Long budgetItemId;
    private TransactionType type;
    private LocalDate transactionDate;
    private String merchant;
    private BigDecimal amount;
    private String note;
}
