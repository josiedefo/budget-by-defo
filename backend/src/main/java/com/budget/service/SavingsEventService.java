package com.budget.service;

import com.budget.dto.LogDepositRequest;
import com.budget.dto.LogWithdrawalRequest;
import com.budget.dto.ReallocateRequest;
import com.budget.dto.SavingsEventDTO;
import com.budget.dto.UpdateSavingsEventRequest;
import com.budget.model.FundGoalType;
import com.budget.model.SavingsEvent;
import com.budget.model.SavingsEventType;
import com.budget.model.SavingsFund;
import com.budget.repository.SavingsAccountRepository;
import com.budget.repository.SavingsEventRepository;
import com.budget.repository.SavingsFundRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsEventService {

    private final SavingsFundRepository savingsFundRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsEventRepository savingsEventRepository;
    private final SavingsFundService savingsFundService;

    public List<SavingsEventDTO> getEventsForFund(Long fundId) {
        return savingsEventRepository.findByFundIdOrderByDateDesc(fundId)
                .stream()
                .map(SavingsEventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SavingsEventDTO logDeposit(LogDepositRequest request) {
        // Determine target fund
        SavingsFund targetFund;
        if (request.getTargetFundId() != null) {
            targetFund = savingsFundRepository.findById(request.getTargetFundId())
                    .orElseThrow(() -> new EntityNotFoundException("Fund not found: " + request.getTargetFundId()));
            if (!targetFund.getIsActive()) {
                throw new IllegalStateException("Cannot deposit into an inactive fund");
            }
        } else {
            targetFund = savingsFundService.getOrCreateUnassignedFund();
        }

        // Pool enforcement: sum of all active fund balances + new deposit <= total pool balance
        BigDecimal currentFundTotal = savingsFundRepository.sumAllActiveFundBalances();
        BigDecimal poolBalance = savingsAccountRepository.sumActiveBalances();
        if (currentFundTotal.add(request.getAmount()).compareTo(poolBalance) > 0) {
            throw new IllegalStateException(
                    "Deposit would exceed savings pool. Pool balance: " + poolBalance +
                    ", current fund total: " + currentFundTotal +
                    ", deposit amount: " + request.getAmount());
        }

        targetFund.setBalance(targetFund.getBalance().add(request.getAmount()));
        savingsFundRepository.save(targetFund);

        SavingsEvent event = buildEvent(targetFund, SavingsEventType.DEPOSIT_ALLOCATED,
                request.getAmount(), request.getEventDate(), request.getNote());
        event = savingsEventRepository.save(event);
        return SavingsEventDTO.fromEntity(event);
    }

    @Transactional
    public SavingsEventDTO logWithdrawal(LogWithdrawalRequest request) {
        SavingsFund fund = savingsFundRepository.findById(request.getFundId())
                .orElseThrow(() -> new EntityNotFoundException("Fund not found: " + request.getFundId()));

        if (fund.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException(
                    "Insufficient fund balance. Fund balance: " + fund.getBalance() +
                    ", withdrawal amount: " + request.getAmount());
        }

        fund.setBalance(fund.getBalance().subtract(request.getAmount()));
        savingsFundRepository.save(fund);

        SavingsEvent event = buildEvent(fund, SavingsEventType.WITHDRAWAL,
                request.getAmount(), request.getEventDate(), request.getNote());
        event = savingsEventRepository.save(event);
        return SavingsEventDTO.fromEntity(event);
    }

    @Transactional
    public List<SavingsEventDTO> reallocate(ReallocateRequest request) {
        if (request.getSourceFundId().equals(request.getDestinationFundId())) {
            throw new IllegalArgumentException("Source and destination funds must be different");
        }

        SavingsFund source = savingsFundRepository.findById(request.getSourceFundId())
                .orElseThrow(() -> new EntityNotFoundException("Source fund not found: " + request.getSourceFundId()));
        SavingsFund destination = savingsFundRepository.findById(request.getDestinationFundId())
                .orElseThrow(() -> new EntityNotFoundException("Destination fund not found: " + request.getDestinationFundId()));

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException(
                    "Insufficient source fund balance. Balance: " + source.getBalance() +
                    ", reallocation amount: " + request.getAmount());
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        savingsFundRepository.save(source);
        savingsFundRepository.save(destination);

        LocalDate eventDate = LocalDate.now();
        SavingsEvent outEvent = buildEvent(source, SavingsEventType.REALLOCATION_OUT,
                request.getAmount(), eventDate, request.getNote());
        SavingsEvent inEvent = buildEvent(destination, SavingsEventType.REALLOCATION_IN,
                request.getAmount(), eventDate, request.getNote());

        outEvent = savingsEventRepository.save(outEvent);
        inEvent = savingsEventRepository.save(inEvent);

        return List.of(SavingsEventDTO.fromEntity(outEvent), SavingsEventDTO.fromEntity(inEvent));
    }

    @Transactional
    public SavingsEventDTO processPayout(Long fundId) {
        SavingsFund fund = savingsFundRepository.findById(fundId)
                .orElseThrow(() -> new EntityNotFoundException("Fund not found: " + fundId));

        if (fund.getGoalType() != FundGoalType.SPEND_DOWN) {
            throw new IllegalStateException("Payout can only be processed for SPEND_DOWN funds");
        }
        if (fund.getPayoutDate() == null || fund.getPayoutDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Payout date has not been reached yet");
        }
        if (fund.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Fund has no balance to pay out");
        }

        BigDecimal payoutAmount = fund.getPayoutAmount() != null
                ? fund.getPayoutAmount().min(fund.getBalance())
                : fund.getBalance();

        fund.setBalance(fund.getBalance().subtract(payoutAmount));
        savingsFundRepository.save(fund);

        SavingsEvent event = buildEvent(fund, SavingsEventType.PAYOUT,
                payoutAmount, LocalDate.now(), "System payout");
        event = savingsEventRepository.save(event);
        return SavingsEventDTO.fromEntity(event);
    }

    @Transactional
    public SavingsEventDTO updateEvent(Long id, UpdateSavingsEventRequest request) {
        SavingsEvent event = savingsEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));

        if (event.getEventType() != SavingsEventType.DEPOSIT_ALLOCATED
                && event.getEventType() != SavingsEventType.WITHDRAWAL) {
            throw new IllegalStateException("Cannot edit this event type");
        }

        SavingsFund fund = event.getFund();
        BigDecimal oldAmount = event.getAmount();
        BigDecimal newAmount = request.getAmount();

        if (event.getEventType() == SavingsEventType.DEPOSIT_ALLOCATED) {
            BigDecimal updatedBalance = fund.getBalance().subtract(oldAmount).add(newAmount);
            // Pool enforcement
            BigDecimal otherFundsTotal = savingsFundRepository.sumAllActiveFundBalances().subtract(fund.getBalance());
            BigDecimal poolBalance = savingsAccountRepository.sumActiveBalances();
            if (otherFundsTotal.add(updatedBalance).compareTo(poolBalance) > 0) {
                throw new IllegalStateException(
                        "Updated amount would exceed savings pool. Pool balance: " + poolBalance);
            }
            fund.setBalance(updatedBalance);
        } else {
            // WITHDRAWAL
            BigDecimal updatedBalance = fund.getBalance().add(oldAmount).subtract(newAmount);
            if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Updated amount would exceed fund balance. Fund balance: " + fund.getBalance());
            }
            fund.setBalance(updatedBalance);
        }

        savingsFundRepository.save(fund);

        event.setAmount(newAmount);
        event.setEventDate(request.getEventDate());
        event.setNote(request.getNote());
        event = savingsEventRepository.save(event);
        return SavingsEventDTO.fromEntity(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        SavingsEvent event = savingsEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));

        if (event.getEventType() != SavingsEventType.DEPOSIT_ALLOCATED
                && event.getEventType() != SavingsEventType.WITHDRAWAL) {
            throw new IllegalStateException("Cannot delete this event type");
        }

        SavingsFund fund = event.getFund();

        if (event.getEventType() == SavingsEventType.DEPOSIT_ALLOCATED) {
            BigDecimal updatedBalance = fund.getBalance().subtract(event.getAmount());
            if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Cannot delete deposit: fund balance would go negative");
            }
            fund.setBalance(updatedBalance);
        } else {
            // WITHDRAWAL — reverse it
            fund.setBalance(fund.getBalance().add(event.getAmount()));
        }

        savingsFundRepository.save(fund);
        savingsEventRepository.delete(event);
    }

    private SavingsEvent buildEvent(SavingsFund fund, SavingsEventType type,
                                     BigDecimal amount, LocalDate eventDate, String note) {
        SavingsEvent event = new SavingsEvent();
        event.setFund(fund);
        event.setEventType(type);
        event.setAmount(amount);
        event.setEventDate(eventDate);
        event.setNote(note);
        return event;
    }
}
