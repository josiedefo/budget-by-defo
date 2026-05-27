package com.budget.dto;

import com.budget.model.SavingsEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LinkTransactionToFundRequest {

    @NotNull
    private Long transactionId;

    @NotNull
    private Long fundId;

    @NotNull
    private SavingsEventType eventType;  // DEPOSIT_ALLOCATED or WITHDRAWAL

    private String note;  // optional — defaults to transaction merchant name
}
