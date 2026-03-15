package com.budget.controller;

import com.budget.dto.AccountDepositRequest;
import com.budget.dto.AccountWithdrawalRequest;
import com.budget.dto.CreateSavingsAccountRequest;
import com.budget.dto.SavingsAccountDTO;
import com.budget.dto.SavingsAccountEventDTO;
import com.budget.dto.UpdateSavingsAccountEventRequest;
import com.budget.dto.UpdateSavingsAccountRequest;
import com.budget.service.SavingsAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savings/accounts")
@RequiredArgsConstructor
public class SavingsAccountController {

    private final SavingsAccountService savingsAccountService;

    @GetMapping
    public List<SavingsAccountDTO> getAllAccounts() {
        return savingsAccountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public SavingsAccountDTO getAccount(@PathVariable Long id) {
        return savingsAccountService.getAccount(id);
    }

    @PostMapping
    public SavingsAccountDTO createAccount(@Valid @RequestBody CreateSavingsAccountRequest request) {
        return savingsAccountService.createAccount(request);
    }

    @PutMapping("/{id}")
    public SavingsAccountDTO updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSavingsAccountRequest request) {
        return savingsAccountService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        savingsAccountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pool-balance")
    public Map<String, BigDecimal> getPoolBalance() {
        return Map.of("totalPoolBalance", savingsAccountService.getTotalPoolBalance());
    }

    @GetMapping("/{id}/events")
    public List<SavingsAccountEventDTO> getAccountEvents(@PathVariable Long id) {
        return savingsAccountService.getEventsForAccount(id);
    }

    @PostMapping("/{id}/deposit")
    public SavingsAccountEventDTO depositToAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountDepositRequest request) {
        return savingsAccountService.logDeposit(id, request);
    }

    @PostMapping("/{id}/withdrawal")
    public SavingsAccountEventDTO withdrawFromAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountWithdrawalRequest request) {
        return savingsAccountService.logWithdrawal(id, request);
    }

    @PutMapping("/events/{eventId}")
    public SavingsAccountEventDTO updateAccountEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateSavingsAccountEventRequest request) {
        return savingsAccountService.updateAccountEvent(eventId, request);
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteAccountEvent(@PathVariable Long eventId) {
        savingsAccountService.deleteAccountEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
