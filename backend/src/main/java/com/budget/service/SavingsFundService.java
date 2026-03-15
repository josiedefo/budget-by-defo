package com.budget.service;

import com.budget.dto.CreateSavingsFundRequest;
import com.budget.dto.SavingsFundDTO;
import com.budget.dto.SavingsSummaryDTO;
import com.budget.dto.UpdateSavingsFundRequest;
import com.budget.model.FundGoalType;
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
public class SavingsFundService {

    private final SavingsFundRepository savingsFundRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsEventRepository savingsEventRepository;

    public List<SavingsFundDTO> getAllFunds() {
        int currentYear = LocalDate.now().getYear();
        return savingsFundRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(fund -> {
                    BigDecimal ytdSpent = null;
                    if (fund.getGoalType() == FundGoalType.SPEND_AS_YOU_GO) {
                        ytdSpent = savingsEventRepository.sumWithdrawalsForFundInYear(fund.getId(), currentYear);
                    }
                    return SavingsFundDTO.fromEntityWithYtd(fund, ytdSpent);
                })
                .collect(Collectors.toList());
    }

    public SavingsFundDTO getFund(Long id) {
        SavingsFund fund = savingsFundRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Savings fund not found: " + id));
        BigDecimal ytdSpent = null;
        if (fund.getGoalType() == FundGoalType.SPEND_AS_YOU_GO) {
            ytdSpent = savingsEventRepository.sumWithdrawalsForFundInYear(fund.getId(), LocalDate.now().getYear());
        }
        return SavingsFundDTO.fromEntityWithYtd(fund, ytdSpent);
    }

    @Transactional
    public SavingsFund getOrCreateUnassignedFund() {
        return savingsFundRepository.findByIsSystemFundTrue().orElseGet(() -> {
            SavingsFund unassigned = new SavingsFund();
            unassigned.setName("Unassigned");
            unassigned.setGoalType(FundGoalType.NO_GOAL);
            unassigned.setBalance(BigDecimal.ZERO);
            unassigned.setIsSystemFund(true);
            unassigned.setIsActive(true);
            return savingsFundRepository.save(unassigned);
        });
    }

    @Transactional
    public SavingsFundDTO createFund(CreateSavingsFundRequest request) {
        validateGoalTypeFields(request.getGoalType(), request.getTargetAmount(),
                request.getDeadline(), request.getCeiling(), request.getPayoutDate(), request.getPayoutAmount());

        SavingsFund fund = new SavingsFund();
        fund.setName(request.getName());
        fund.setGoalType(request.getGoalType());
        fund.setBalance(BigDecimal.ZERO);
        fund.setTargetAmount(request.getTargetAmount());
        fund.setDeadline(request.getDeadline());
        fund.setCeiling(request.getCeiling());
        fund.setPayoutDate(request.getPayoutDate());
        fund.setPayoutAmount(request.getPayoutAmount());
        fund.setIsSystemFund(false);
        fund.setIsActive(true);
        fund = savingsFundRepository.save(fund);
        return SavingsFundDTO.fromEntity(fund);
    }

    @Transactional
    public SavingsFundDTO updateFund(Long id, UpdateSavingsFundRequest request) {
        SavingsFund fund = savingsFundRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Savings fund not found: " + id));

        if (fund.getIsSystemFund() && request.getName() != null) {
            throw new IllegalStateException("Cannot rename the Unassigned system fund");
        }

        if (request.getName() != null) {
            fund.setName(request.getName());
        }
        if (request.getGoalType() != null) {
            fund.setGoalType(request.getGoalType());
        }
        fund.setTargetAmount(request.getTargetAmount());
        fund.setDeadline(request.getDeadline());
        fund.setCeiling(request.getCeiling());
        fund.setPayoutDate(request.getPayoutDate());
        fund.setPayoutAmount(request.getPayoutAmount());

        fund = savingsFundRepository.save(fund);
        return SavingsFundDTO.fromEntity(fund);
    }

    @Transactional
    public void deleteFund(Long id) {
        SavingsFund fund = savingsFundRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Savings fund not found: " + id));

        if (fund.getIsSystemFund()) {
            throw new IllegalStateException("Cannot delete the Unassigned system fund");
        }
        if (fund.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot delete a fund with remaining balance. Reallocate the balance first.");
        }

        fund.setIsActive(false);
        savingsFundRepository.save(fund);
    }

    public SavingsSummaryDTO getYearSummary() {
        BigDecimal totalPool = savingsAccountRepository.sumActiveBalances();

        SavingsFund unassigned = savingsFundRepository.findByIsSystemFundTrue().orElse(null);
        BigDecimal totalUnallocated = unassigned != null ? unassigned.getBalance() : BigDecimal.ZERO;

        List<SavingsFund> allActive = savingsFundRepository.findAllByIsActiveTrueOrderByNameAsc();
        BigDecimal totalAllocated = allActive.stream()
                .filter(f -> !f.getIsSystemFund())
                .map(SavingsFund::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemainingToSave = allActive.stream()
                .filter(f -> !f.getIsSystemFund()
                        && (f.getGoalType() == FundGoalType.TARGET
                            || f.getGoalType() == FundGoalType.TARGET_WITH_DEADLINE
                            || f.getGoalType() == FundGoalType.SPEND_DOWN)
                        && f.getTargetAmount() != null
                        && f.getBalance().compareTo(f.getTargetAmount()) < 0)
                .map(f -> f.getTargetAmount().subtract(f.getBalance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SavingsFundDTO> upcomingDeadlines = savingsFundRepository.findUpcomingDeadlineFunds()
                .stream()
                .map(SavingsFundDTO::fromEntity)
                .collect(Collectors.toList());

        return new SavingsSummaryDTO(totalPool, totalAllocated, totalUnallocated, totalRemainingToSave, upcomingDeadlines);
    }

    private void validateGoalTypeFields(FundGoalType goalType, BigDecimal targetAmount,
                                         LocalDate deadline, BigDecimal ceiling,
                                         LocalDate payoutDate, BigDecimal payoutAmount) {
        switch (goalType) {
            case TARGET:
                if (targetAmount == null) throw new IllegalArgumentException("targetAmount is required for TARGET funds");
                break;
            case TARGET_WITH_DEADLINE:
                if (targetAmount == null) throw new IllegalArgumentException("targetAmount is required for TARGET_WITH_DEADLINE funds");
                if (deadline == null) throw new IllegalArgumentException("deadline is required for TARGET_WITH_DEADLINE funds");
                break;
            case SPEND_DOWN:
                if (targetAmount == null) throw new IllegalArgumentException("targetAmount is required for SPEND_DOWN funds");
                if (payoutDate == null) throw new IllegalArgumentException("payoutDate is required for SPEND_DOWN funds");
                if (payoutAmount == null) throw new IllegalArgumentException("payoutAmount is required for SPEND_DOWN funds");
                break;
            case SPEND_AS_YOU_GO:
                if (ceiling == null) throw new IllegalArgumentException("ceiling is required for SPEND_AS_YOU_GO funds");
                break;
            default:
                break;
        }
    }
}
