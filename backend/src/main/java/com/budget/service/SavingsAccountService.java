package com.budget.service;

import com.budget.dto.AccountDepositRequest;
import com.budget.dto.AccountWithdrawalRequest;
import com.budget.dto.BulkLinkBudgetItemRequest;
import com.budget.dto.BulkLinkResult;
import com.budget.dto.BulkLinkTransactionsRequest;
import com.budget.dto.CreateSavingsAccountRequest;
import com.budget.dto.LinkTransactionToAccountRequest;
import com.budget.dto.SavingsAccountDTO;
import com.budget.dto.SavingsAccountEventDTO;
import com.budget.dto.UpdateSavingsAccountEventRequest;
import com.budget.dto.UpdateSavingsAccountRequest;
import com.budget.model.SavingsAccount;
import com.budget.model.SavingsAccountEvent;
import com.budget.model.SavingsAccountEventType;
import com.budget.model.Transaction;
import com.budget.repository.SavingsAccountEventRepository;
import com.budget.repository.SavingsAccountRepository;
import com.budget.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountEventRepository savingsAccountEventRepository;
    private final TransactionRepository transactionRepository;

    public List<SavingsAccountDTO> getAllAccounts() {
        List<SavingsAccount> accounts = savingsAccountRepository.findAllByIsActiveTrueOrderByNameAsc();
        List<Long> ids = accounts.stream().map(SavingsAccount::getId).collect(Collectors.toList());
        Map<Long, LocalDate> latestEventDates = savingsAccountEventRepository
                .findLatestEventDatesForAccounts(ids)
                .stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDate) row[1]));
        return accounts.stream()
                .map(a -> {
                    SavingsAccountDTO dto = SavingsAccountDTO.fromEntity(a);
                    LocalDate latestEvent = latestEventDates.get(a.getId());
                    if (latestEvent != null && (a.getAsOfDate() == null || latestEvent.isAfter(a.getAsOfDate()))) {
                        dto.setAsOfDate(latestEvent);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public SavingsAccountDTO getAccount(Long id) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + id));
        SavingsAccountDTO dto = SavingsAccountDTO.fromEntity(account);
        LocalDate latestEvent = savingsAccountEventRepository.findLatestEventDateByAccountId(id);
        if (latestEvent != null && (account.getAsOfDate() == null || latestEvent.isAfter(account.getAsOfDate()))) {
            dto.setAsOfDate(latestEvent);
        }
        return dto;
    }

    @Transactional
    public SavingsAccountDTO createAccount(CreateSavingsAccountRequest request) {
        SavingsAccount account = new SavingsAccount();
        account.setName(request.getName());
        account.setBalance(request.getBalance());
        account.setAsOfDate(request.getAsOfDate());
        account.setIsActive(true);
        account = savingsAccountRepository.save(account);

        // Log initial balance as a deposit if non-zero
        if (request.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            SavingsAccountEvent event = new SavingsAccountEvent();
            event.setAccount(account);
            event.setEventType(SavingsAccountEventType.DEPOSIT);
            event.setAmount(request.getBalance());
            event.setBalanceAfter(request.getBalance());
            event.setEventDate(request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now());
            event.setNote("Initial balance");
            savingsAccountEventRepository.save(event);
        }

        return SavingsAccountDTO.fromEntity(account);
    }

    @Transactional
    public SavingsAccountDTO updateAccount(Long id, UpdateSavingsAccountRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + id));
        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getAsOfDate() != null) {
            account.setAsOfDate(request.getAsOfDate());
        }

        if (request.getBalance() != null) {
            BigDecimal oldBalance = account.getBalance();
            BigDecimal newBalance = request.getBalance();
            int cmp = newBalance.compareTo(oldBalance);
            account.setBalance(newBalance);
            account = savingsAccountRepository.save(account);

            if (cmp != 0) {
                BigDecimal delta = newBalance.subtract(oldBalance).abs();
                SavingsAccountEventType type = cmp > 0
                        ? SavingsAccountEventType.DEPOSIT
                        : SavingsAccountEventType.WITHDRAWAL;
                SavingsAccountEvent event = new SavingsAccountEvent();
                event.setAccount(account);
                event.setEventType(type);
                event.setAmount(delta);
                event.setBalanceAfter(newBalance);
                event.setEventDate(request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now());
                event.setNote("Balance adjustment");
                savingsAccountEventRepository.save(event);
            }
        } else {
            account = savingsAccountRepository.save(account);
        }

        return SavingsAccountDTO.fromEntity(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + id));
        account.setIsActive(false);
        savingsAccountRepository.save(account);
    }

    public BigDecimal getTotalPoolBalance() {
        return savingsAccountRepository.sumActiveBalances();
    }

    public List<SavingsAccountEventDTO> getEventsForAccount(Long accountId) {
        savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + accountId));

        List<SavingsAccountEvent> events = savingsAccountEventRepository.findByAccountIdOrderByDateAsc(accountId);

        // Recompute balanceAfter chronologically so it's correct regardless of insertion order
        BigDecimal running = BigDecimal.ZERO;
        List<SavingsAccountEventDTO> dtos = new java.util.ArrayList<>();
        for (SavingsAccountEvent event : events) {
            running = event.getEventType() == SavingsAccountEventType.DEPOSIT
                    ? running.add(event.getAmount())
                    : running.subtract(event.getAmount());
            SavingsAccountEventDTO dto = SavingsAccountEventDTO.fromEntity(event);
            dto.setBalanceAfter(running);
            dtos.add(dto);
        }

        java.util.Collections.reverse(dtos);
        return dtos;
    }

    @Transactional
    public SavingsAccountEventDTO logDeposit(Long accountId, AccountDepositRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + accountId));

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        account.setAsOfDate(request.getEventDate());
        account = savingsAccountRepository.save(account);

        SavingsAccountEvent event = new SavingsAccountEvent();
        event.setAccount(account);
        event.setEventType(SavingsAccountEventType.DEPOSIT);
        event.setAmount(request.getAmount());
        event.setBalanceAfter(newBalance);
        event.setEventDate(request.getEventDate());
        event.setNote(request.getNote());
        event = savingsAccountEventRepository.save(event);

        return SavingsAccountEventDTO.fromEntity(event);
    }

    @Transactional
    public SavingsAccountEventDTO updateAccountEvent(Long id, UpdateSavingsAccountEventRequest request) {
        SavingsAccountEvent event = savingsAccountEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account event not found: " + id));

        SavingsAccount account = event.getAccount();
        BigDecimal oldAmount = event.getAmount();
        BigDecimal newAmount = request.getAmount();
        BigDecimal delta = newAmount.subtract(oldAmount);

        if (event.getEventType() == SavingsAccountEventType.DEPOSIT) {
            BigDecimal updatedBalance = account.getBalance().add(delta);
            if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Updated amount would result in negative account balance");
            }
            account.setBalance(updatedBalance);
        } else {
            BigDecimal updatedBalance = account.getBalance().subtract(delta);
            if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Updated amount would result in negative account balance");
            }
            account.setBalance(updatedBalance);
        }

        account = savingsAccountRepository.save(account);
        event.setAmount(newAmount);
        event.setBalanceAfter(account.getBalance());
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getNote() != null) {
            event.setNote(request.getNote());
        }
        event = savingsAccountEventRepository.save(event);
        return SavingsAccountEventDTO.fromEntity(event);
    }

    @Transactional
    public void deleteAccountEvent(Long id) {
        SavingsAccountEvent event = savingsAccountEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account event not found: " + id));

        SavingsAccount account = event.getAccount();

        if (event.getEventType() == SavingsAccountEventType.DEPOSIT) {
            BigDecimal updatedBalance = account.getBalance().subtract(event.getAmount());
            if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Cannot delete deposit: account balance would go negative");
            }
            account.setBalance(updatedBalance);
        } else {
            account.setBalance(account.getBalance().add(event.getAmount()));
        }

        savingsAccountRepository.save(account);
        savingsAccountEventRepository.delete(event);
    }

    @Transactional
    public SavingsAccountEventDTO logWithdrawal(Long accountId, AccountWithdrawalRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + accountId));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient account balance");
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        account.setAsOfDate(request.getEventDate());
        account = savingsAccountRepository.save(account);

        SavingsAccountEvent event = new SavingsAccountEvent();
        event.setAccount(account);
        event.setEventType(SavingsAccountEventType.WITHDRAWAL);
        event.setAmount(request.getAmount());
        event.setBalanceAfter(newBalance);
        event.setEventDate(request.getEventDate());
        event.setNote(request.getNote());
        event = savingsAccountEventRepository.save(event);

        return SavingsAccountEventDTO.fromEntity(event);
    }

    @Transactional
    public SavingsAccountEventDTO linkTransactionToAccount(Long accountId, LinkTransactionToAccountRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + accountId));

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + request.getTransactionId()));

        // Reject if transaction is already linked to any savings account event
        if (savingsAccountEventRepository.findByTransactionId(request.getTransactionId()).isPresent()) {
            throw new IllegalStateException("This transaction is already linked to a savings account event");
        }

        BigDecimal amount = transaction.getAmount();
        SavingsAccountEventType eventType = request.getEventType();

        if (eventType == SavingsAccountEventType.WITHDRAWAL && account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient account balance for withdrawal");
        }

        BigDecimal newBalance = eventType == SavingsAccountEventType.DEPOSIT
                ? account.getBalance().add(amount)
                : account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        account.setAsOfDate(transaction.getTransactionDate());
        account = savingsAccountRepository.save(account);

        String note = (request.getNote() != null && !request.getNote().isBlank())
                ? request.getNote()
                : transaction.getMerchant();

        SavingsAccountEvent event = new SavingsAccountEvent();
        event.setAccount(account);
        event.setEventType(eventType);
        event.setAmount(amount);
        event.setBalanceAfter(newBalance);
        event.setEventDate(transaction.getTransactionDate());
        event.setNote(note);
        event.setTransaction(transaction);
        event = savingsAccountEventRepository.save(event);

        return SavingsAccountEventDTO.fromEntity(event);
    }

    @Transactional
    public BulkLinkResult bulkLinkBudgetItem(Long accountId, BulkLinkBudgetItemRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + accountId));

        // Load transactions for this budget item (optionally scoped to a date range)
        List<Transaction> transactions = (request.getStartDate() != null && request.getEndDate() != null)
                ? transactionRepository.findByBudgetItemIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        request.getBudgetItemId(), request.getStartDate(), request.getEndDate())
                : transactionRepository.findByBudgetItemIdOrderByTransactionDateDesc(request.getBudgetItemId());

        return performBulkLink(account, transactions, request.getEventType(), request.getNote());
    }

    /**
     * Links an arbitrary, explicitly-chosen set of transactions (e.g. a multi-select on the
     * Transactions page) rather than everything under one budget item.
     */
    @Transactional
    public BulkLinkResult bulkLinkTransactions(Long accountId, BulkLinkTransactionsRequest request) {
        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Savings account not found: " + accountId));

        List<Transaction> transactions = transactionRepository.findAllById(request.getTransactionIds());
        return performBulkLink(account, transactions, request.getEventType(), request.getNote());
    }

    /**
     * Shared bulk-link core: skips transactions already linked to any savings account event,
     * pre-flight-checks withdrawals against the account balance, then creates one event per
     * remaining transaction. Used by both the budget-item-scoped and explicit-transaction-list
     * bulk-link entry points.
     */
    private BulkLinkResult performBulkLink(SavingsAccount account, List<Transaction> transactions,
                                            SavingsAccountEventType eventType, String note) {
        if (transactions.isEmpty()) {
            return new BulkLinkResult(0, 0, 0, BigDecimal.ZERO);
        }

        // Find which transactions are already linked to any savings account event
        List<Long> txIds = transactions.stream().map(Transaction::getId).collect(Collectors.toList());
        Set<Long> alreadyLinked = savingsAccountEventRepository.findByTransactionIdIn(txIds)
                .stream()
                .map(e -> e.getTransaction().getId())
                .collect(Collectors.toSet());

        List<Transaction> toLink = transactions.stream()
                .filter(tx -> !alreadyLinked.contains(tx.getId()))
                .collect(Collectors.toList());

        int skipped = alreadyLinked.size();

        if (toLink.isEmpty()) {
            return new BulkLinkResult(0, skipped, transactions.size(), BigDecimal.ZERO);
        }

        // Pre-flight balance check for withdrawals
        if (eventType == SavingsAccountEventType.WITHDRAWAL) {
            BigDecimal total = toLink.stream().map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (account.getBalance().compareTo(total) < 0) {
                throw new IllegalStateException(
                        "Insufficient account balance for bulk withdrawal. " +
                        "Balance: " + account.getBalance() + ", total: " + total);
            }
        }

        BigDecimal totalLinked = BigDecimal.ZERO;
        String noteTemplate = (note != null && !note.isBlank()) ? note : null;

        for (Transaction tx : toLink) {
            BigDecimal newBalance = eventType == SavingsAccountEventType.DEPOSIT
                    ? account.getBalance().add(tx.getAmount())
                    : account.getBalance().subtract(tx.getAmount());
            account.setBalance(newBalance);
            account.setAsOfDate(tx.getTransactionDate());

            SavingsAccountEvent event = new SavingsAccountEvent();
            event.setAccount(account);
            event.setEventType(eventType);
            event.setAmount(tx.getAmount());
            event.setBalanceAfter(newBalance);
            event.setEventDate(tx.getTransactionDate());
            event.setNote(noteTemplate != null ? noteTemplate : tx.getMerchant());
            event.setTransaction(tx);
            savingsAccountEventRepository.save(event);

            totalLinked = totalLinked.add(tx.getAmount());
        }

        savingsAccountRepository.save(account);

        return new BulkLinkResult(toLink.size(), skipped, transactions.size(), totalLinked);
    }
}
