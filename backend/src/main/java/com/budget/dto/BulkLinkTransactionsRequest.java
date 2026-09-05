package com.budget.dto;

import com.budget.model.SavingsAccountEventType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BulkLinkTransactionsRequest {

    @NotEmpty
    private List<Long> transactionIds;

    @NotNull
    private SavingsAccountEventType eventType;  // DEPOSIT or WITHDRAWAL

    private String note;  // optional — defaults to each transaction's merchant name
}
