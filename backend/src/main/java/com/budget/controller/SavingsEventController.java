package com.budget.controller;

import com.budget.dto.LogDepositRequest;
import com.budget.dto.LogWithdrawalRequest;
import com.budget.dto.ReallocateRequest;
import com.budget.dto.SavingsEventDTO;
import com.budget.dto.UpdateSavingsEventRequest;
import com.budget.service.SavingsEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings/events")
@RequiredArgsConstructor
public class SavingsEventController {

    private final SavingsEventService savingsEventService;

    @GetMapping("/fund/{fundId}")
    public List<SavingsEventDTO> getEventsForFund(@PathVariable Long fundId) {
        return savingsEventService.getEventsForFund(fundId);
    }

    @PostMapping("/deposit")
    public SavingsEventDTO logDeposit(@Valid @RequestBody LogDepositRequest request) {
        return savingsEventService.logDeposit(request);
    }

    @PostMapping("/withdrawal")
    public SavingsEventDTO logWithdrawal(@Valid @RequestBody LogWithdrawalRequest request) {
        return savingsEventService.logWithdrawal(request);
    }

    @PostMapping("/reallocate")
    public List<SavingsEventDTO> reallocate(@Valid @RequestBody ReallocateRequest request) {
        return savingsEventService.reallocate(request);
    }

    @PostMapping("/payout/{fundId}")
    public SavingsEventDTO processPayout(@PathVariable Long fundId) {
        return savingsEventService.processPayout(fundId);
    }

    @PutMapping("/{id}")
    public SavingsEventDTO updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSavingsEventRequest request) {
        return savingsEventService.updateEvent(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        savingsEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
