package com.budget.dto;

import com.budget.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTransactionRequest {
    private Long sectionId;
    private Long budgetItemId;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Date is required")
    private LocalDate transactionDate;

    @NotBlank(message = "Merchant is required")
    private String merchant;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Note is required")
    private String note;
}
