package com.budget.dto;

import com.budget.model.SavingsEventType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BulkLinkTransactionsToFundRequest {

    @NotEmpty
    private List<Long> transactionIds;

    @NotNull
    private Long fundId;

    @NotNull
    private SavingsEventType eventType;  // DEPOSIT_ALLOCATED or WITHDRAWAL

    private String note;  // optional — defaults to each transaction's merchant name
}
