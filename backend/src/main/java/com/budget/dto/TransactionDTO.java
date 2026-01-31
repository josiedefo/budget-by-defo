package com.budget.dto;

import com.budget.model.Transaction;
import com.budget.model.TransactionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TransactionDTO {
    private Long id;
    private Long sectionId;
    private String sectionName;
    private Long budgetItemId;
    private String budgetItemName;
    private TransactionType type;
    private LocalDate transactionDate;
    private String merchant;
    private BigDecimal amount;
    private String note;
    private LocalDateTime createdAt;

    public static TransactionDTO fromEntity(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setType(transaction.getType());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setMerchant(transaction.getMerchant());
        dto.setAmount(transaction.getAmount());
        dto.setNote(transaction.getNote());
        dto.setCreatedAt(transaction.getCreatedAt());

        if (transaction.getSection() != null) {
            dto.setSectionId(transaction.getSection().getId());
            dto.setSectionName(transaction.getSection().getName());
        }
        if (transaction.getBudgetItem() != null) {
            dto.setBudgetItemId(transaction.getBudgetItem().getId());
            dto.setBudgetItemName(transaction.getBudgetItem().getName());
        }

        return dto;
    }
}
