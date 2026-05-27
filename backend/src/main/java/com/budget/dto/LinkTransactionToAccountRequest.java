package com.budget.dto;

import com.budget.model.SavingsAccountEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LinkTransactionToAccountRequest {

    @NotNull
    private Long transactionId;

    @NotNull
    private SavingsAccountEventType eventType;  // DEPOSIT or WITHDRAWAL

    private String note;  // optional — defaults to transaction merchant name
}
