package com.budget.service;

import com.budget.dto.BudgetItemLinkStatus;
import com.budget.model.SavingsAccountEvent;
import com.budget.model.SavingsEvent;
import com.budget.model.Transaction;
import com.budget.repository.SavingsAccountEventRepository;
import com.budget.repository.SavingsEventRepository;
import com.budget.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsLinkStatusService {

    private final TransactionRepository transactionRepository;
    private final SavingsAccountEventRepository savingsAccountEventRepository;
    private final SavingsEventRepository savingsEventRepository;

    /**
     * Returns the savings link status for each of the requested budget items,
     * scoped to the given date range (i.e. the current month).
     */
    @Transactional(readOnly = true)
    public Map<Long, BudgetItemLinkStatus> getBatchLinkStatus(
            List<Long> budgetItemIds, LocalDate startDate, LocalDate endDate) {

        Map<Long, BudgetItemLinkStatus> result = new HashMap<>();

        for (Long budgetItemId : budgetItemIds) {
            List<Transaction> transactions = transactionRepository
                    .findByBudgetItemIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                            budgetItemId, startDate, endDate);

            if (transactions.isEmpty()) {
                result.put(budgetItemId, new BudgetItemLinkStatus(0, false, null, null, false, null, null));
                continue;
            }

            List<Long> txIds = transactions.stream().map(Transaction::getId).collect(Collectors.toList());

            // ── Account link check ──
            List<SavingsAccountEvent> accountEvents = savingsAccountEventRepository.findByTransactionIdIn(txIds);
            boolean allLinkedToAccount = accountEvents.size() == transactions.size();
            Long accountId = null;
            String accountName = null;
            if (allLinkedToAccount) {
                Set<Long> accountIds = accountEvents.stream()
                        .map(e -> e.getAccount().getId())
                        .collect(Collectors.toSet());
                if (accountIds.size() == 1) {
                    SavingsAccountEvent first = accountEvents.get(0);
                    accountId = first.getAccount().getId();
                    accountName = first.getAccount().getName();
                } else {
                    allLinkedToAccount = false; // linked to different accounts
                }
            }

            // ── Fund link check ──
            List<SavingsEvent> fundEvents = savingsEventRepository.findByTransactionRefIn(txIds);
            boolean allLinkedToFund = fundEvents.size() == transactions.size();
            Long fundId = null;
            String fundName = null;
            if (allLinkedToFund) {
                Set<Long> fundIds = fundEvents.stream()
                        .map(e -> e.getFund().getId())
                        .collect(Collectors.toSet());
                if (fundIds.size() == 1) {
                    SavingsEvent first = fundEvents.get(0);
                    fundId = first.getFund().getId();
                    fundName = first.getFund().getName();
                } else {
                    allLinkedToFund = false; // linked to different funds
                }
            }

            result.put(budgetItemId, new BudgetItemLinkStatus(
                    transactions.size(),
                    allLinkedToAccount, accountId, accountName,
                    allLinkedToFund, fundId, fundName));
        }

        return result;
    }
}
