package com.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetItemLinkStatus {

    private int totalTransactions;

    // true only when ALL transactions are linked to the same savings account
    private boolean allLinkedToAccount;
    private Long accountId;
    private String accountName;

    // true only when ALL transactions are linked to the same savings fund
    private boolean allLinkedToFund;
    private Long fundId;
    private String fundName;
}
