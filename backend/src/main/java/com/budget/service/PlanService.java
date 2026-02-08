package com.budget.service;

import com.budget.dto.CreatePlanRequest;
import com.budget.dto.PlanDTO;
import com.budget.dto.UpdatePlanRequest;
import com.budget.model.BudgetItem;
import com.budget.model.Plan;
import com.budget.model.PlanItem;
import com.budget.repository.BudgetItemRepository;
import com.budget.repository.PlanItemRepository;
import com.budget.repository.PlanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanItemRepository planItemRepository;
    private final BudgetItemRepository budgetItemRepository;

    @Transactional(readOnly = true)
    public List<PlanDTO> getPlansForMonth(Integer year, Integer month) {
        return planRepository.findByYearAndMonthOrderByIdAsc(year, month).stream()
                .map(PlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanDTO getPlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + id));
        return PlanDTO.fromEntity(plan);
    }

    @Transactional(readOnly = true)
    public PlanDTO getPlanByBudgetItem(Long budgetItemId, Integer year, Integer month) {
        return planRepository.findByBudgetItemIdAndYearAndMonth(budgetItemId, year, month)
                .map(PlanDTO::fromEntity)
                .orElse(null);
    }

    @Transactional
    public PlanDTO createPlan(CreatePlanRequest request) {
        BudgetItem budgetItem = budgetItemRepository.findById(request.getBudgetItemId())
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found with id: " + request.getBudgetItemId()));

        if (planRepository.existsByBudgetItemIdAndYearAndMonth(
                request.getBudgetItemId(), request.getYear(), request.getMonth())) {
            throw new IllegalStateException("A plan already exists for this budget item in " +
                    request.getYear() + "/" + request.getMonth());
        }

        Plan plan = new Plan();
        plan.setBudgetItem(budgetItem);
        plan.setYear(request.getYear());
        plan.setMonth(request.getMonth());

        plan = planRepository.save(plan);
        return PlanDTO.fromEntity(plan);
    }

    @Transactional
    public PlanDTO updatePlan(Long id, UpdatePlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + id));

        // Clear existing items
        planItemRepository.deleteByPlanId(id);
        plan.getItems().clear();

        // Add new items
        int order = 0;
        for (UpdatePlanRequest.PlanItemInput input : request.getItems()) {
            PlanItem item = new PlanItem();
            item.setPlan(plan);
            item.setName(input.getName());
            item.setAmount(input.getAmount() != null ? input.getAmount() : BigDecimal.ZERO);
            item.setDisplayOrder(order++);
            item.setFromSubscription(input.getFromSubscription() != null && input.getFromSubscription());
            plan.getItems().add(item);
        }

        plan = planRepository.save(plan);

        // Calculate total and update budget item's planned amount
        BigDecimal total = plan.getItems().stream()
                .map(PlanItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BudgetItem budgetItem = plan.getBudgetItem();
        budgetItem.setPlannedAmount(total);
        budgetItemRepository.save(budgetItem);

        return PlanDTO.fromEntity(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + id));

        // Reset budget item's planned amount to zero
        BudgetItem budgetItem = plan.getBudgetItem();
        budgetItem.setPlannedAmount(BigDecimal.ZERO);
        budgetItemRepository.save(budgetItem);

        planRepository.delete(plan);
    }
}
