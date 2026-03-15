package com.budget.service;

import com.budget.dto.AccountDepositRequest;
import com.budget.dto.AccountWithdrawalRequest;
import com.budget.dto.CreateSavingsAccountRequest;
import com.budget.dto.SavingsAccountDTO;
import com.budget.dto.SavingsAccountEventDTO;
import com.budget.dto.UpdateSavingsAccountEventRequest;
import com.budget.dto.UpdateSavingsAccountRequest;
import com.budget.model.SavingsAccount;
import com.budget.model.SavingsAccountEvent;
import com.budget.model.SavingsAccountEventType;
import com.budget.repository.SavingsAccountEventRepository;
import com.budget.repository.SavingsAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountEventRepository savingsAccountEventRepository;

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
        return savingsAccountEventRepository.findByAccountIdOrderByDateDesc(accountId)
                .stream()
                .map(SavingsAccountEventDTO::fromEntity)
                .collect(Collectors.toList());
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
        event.setEventDate(request.getEventDate());
        event.setNote(request.getNote());
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
}
