package com.budget.service;

import com.budget.dto.CreateSalaryRequest;
import com.budget.dto.SalaryDTO;
import com.budget.dto.UpdateSalaryRequest;
import com.budget.model.BudgetItem;
import com.budget.model.PlanItem;
import com.budget.model.Salary;
import com.budget.repository.BudgetItemRepository;
import com.budget.repository.PlanItemRepository;
import com.budget.repository.PlanRepository;
import com.budget.repository.SalaryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryService {

    private final SalaryRepository salaryRepository;
    private final PlanItemRepository planItemRepository;
    private final PlanRepository planRepository;
    private final BudgetItemRepository budgetItemRepository;

    public List<SalaryDTO> getAllSalaries() {
        return salaryRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(SalaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public SalaryDTO getSalary(Long id) {
        Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salary not found: " + id));
        return SalaryDTO.fromEntity(salary);
    }

    @Transactional
    public SalaryDTO createSalary(CreateSalaryRequest request) {
        Salary salary = new Salary();
        salary.setName(request.getName());
        salary.setRegularAmount(request.getRegularAmount());
        salary.setFederalTax(request.getFederalTax());
        salary.setMedicare(request.getMedicare());
        salary.setSocialSecurity(request.getSocialSecurity());
        salary.setFourOhOneK(request.getFourOhOneK());
        salary.setExtraTaxWithholding(request.getExtraTaxWithholding());
        salary.setHealthSavingsAccount(request.getHealthSavingsAccount());
        salary.setMedicalInsurance(request.getMedicalInsurance());
        salary.setFlexibleSpendingAccount(request.getFlexibleSpendingAccount());
        salary.setIsActive(true);

        salary = salaryRepository.save(salary);
        return SalaryDTO.fromEntity(salary);
    }

    @Transactional
    public SalaryDTO updateSalary(Long id, UpdateSalaryRequest request) {
        Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salary not found: " + id));

        // Capture old name BEFORE updating — used to find name-matched legacy plan items
        String oldName = salary.getName();

        if (request.getName() != null) {
            salary.setName(request.getName());
        }
        if (request.getRegularAmount() != null) {
            salary.setRegularAmount(request.getRegularAmount());
        }
        if (request.getFederalTax() != null) {
            salary.setFederalTax(request.getFederalTax());
        }
        if (request.getMedicare() != null) {
            salary.setMedicare(request.getMedicare());
        }
        if (request.getSocialSecurity() != null) {
            salary.setSocialSecurity(request.getSocialSecurity());
        }
        // Optional fields - update even if null (allows clearing)
        salary.setFourOhOneK(request.getFourOhOneK());
        salary.setExtraTaxWithholding(request.getExtraTaxWithholding());
        salary.setHealthSavingsAccount(request.getHealthSavingsAccount());
        salary.setMedicalInsurance(request.getMedicalInsurance());
        salary.setFlexibleSpendingAccount(request.getFlexibleSpendingAccount());

        salary = salaryRepository.save(salary);
        propagateSalaryUpdate(salary, oldName);
        return SalaryDTO.fromEntity(salary);
    }

    /**
     * When a salary's name or deductions change, update all linked plan items with the
     * new name and recomputed net pay, then sync plan totals → budget item planned amounts.
     * Handles both FK-linked items (new) and legacy items matched by flag+oldName (existing plans).
     * Backfills the FK on legacy items so future updates work via FK alone.
     */
    private void propagateSalaryUpdate(Salary salary, String oldName) {
        List<PlanItem> linkedItems = planItemRepository.findLinkedToSalary(salary.getId(), oldName);
        if (linkedItems.isEmpty()) return;

        BigDecimal netPay = SalaryDTO.computeNetPay(salary);
        for (PlanItem item : linkedItems) {
            item.setName(salary.getName());
            item.setAmount(netPay);
            // Backfill the FK so future updates flow through the FK path
            if (item.getSalary() == null) {
                item.setSalary(salary);
            }
        }
        planItemRepository.saveAll(linkedItems);

        Set<Long> planIds = linkedItems.stream()
                .map(pi -> pi.getPlan().getId())
                .collect(Collectors.toSet());
        for (Long planId : planIds) {
            recalculatePlanTotal(planId);
        }
    }

    private void recalculatePlanTotal(Long planId) {
        List<PlanItem> allItems = planItemRepository.findByPlanIdOrderByDisplayOrderAsc(planId);
        BigDecimal total = allItems.stream()
                .map(PlanItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        planRepository.findById(planId).ifPresent(plan -> {
            BudgetItem budgetItem = plan.getBudgetItem();
            budgetItem.setPlannedAmount(total);
            budgetItemRepository.save(budgetItem);
        });
    }

    @Transactional
    public void deleteSalary(Long id) {
        Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salary not found: " + id));

        // Soft delete - just mark as inactive
        salary.setIsActive(false);
        salaryRepository.save(salary);
    }
}
