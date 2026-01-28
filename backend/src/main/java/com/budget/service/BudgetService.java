package com.budget.service;

import com.budget.dto.BudgetDTO;
import com.budget.dto.YearlySummaryDTO;
import com.budget.model.Budget;
import com.budget.model.Section;
import com.budget.repository.BudgetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;

    private static final String[] DEFAULT_SECTIONS = {
            "Income", "Housing", "Transportation", "Food",
            "Utilities", "Healthcare", "Entertainment", "Savings"
    };

    @Transactional(readOnly = true)
    public BudgetDTO getBudget(Integer year, Integer month) {
        Budget budget = budgetRepository.findByYearAndMonthWithSectionsAndItems(year, month)
                .orElse(null);

        if (budget == null) {
            return null;
        }

        return BudgetDTO.fromEntity(budget);
    }

    @Transactional
    public BudgetDTO getOrCreateBudget(Integer year, Integer month) {
        Budget budget = budgetRepository.findByYearAndMonthWithSectionsAndItems(year, month)
                .orElseGet(() -> createBudgetWithDefaults(year, month));

        return BudgetDTO.fromEntity(budget);
    }

    @Transactional
    public BudgetDTO createBudget(Integer year, Integer month) {
        if (budgetRepository.existsByYearAndMonth(year, month)) {
            throw new IllegalArgumentException("Budget already exists for " + year + "/" + month);
        }

        Budget budget = createBudgetWithDefaults(year, month);
        return BudgetDTO.fromEntity(budget);
    }

    private Budget createBudgetWithDefaults(Integer year, Integer month) {
        Budget budget = new Budget(year, month);
        budget.setSections(new ArrayList<>());

        for (int i = 0; i < DEFAULT_SECTIONS.length; i++) {
            Section section = new Section();
            section.setName(DEFAULT_SECTIONS[i]);
            section.setDisplayOrder(i + 1);
            section.setIsIncome(DEFAULT_SECTIONS[i].equals("Income"));
            section.setBudget(budget);
            section.setItems(new ArrayList<>());
            budget.getSections().add(section);
        }

        return budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public YearlySummaryDTO getYearlySummary(Integer year) {
        List<Budget> budgets = budgetRepository.findByYearOrderByMonthAsc(year);

        YearlySummaryDTO summary = new YearlySummaryDTO();
        summary.setYear(year);

        List<YearlySummaryDTO.MonthSummaryDTO> months = new ArrayList<>();

        BigDecimal totalPlannedIncome = BigDecimal.ZERO;
        BigDecimal totalActualIncome = BigDecimal.ZERO;
        BigDecimal totalPlannedExpenses = BigDecimal.ZERO;
        BigDecimal totalActualExpenses = BigDecimal.ZERO;

        for (Budget budget : budgets) {
            Budget fullBudget = budgetRepository.findByIdWithSectionsAndItems(budget.getId())
                    .orElse(budget);

            BudgetDTO budgetDTO = BudgetDTO.fromEntity(fullBudget);

            YearlySummaryDTO.MonthSummaryDTO monthSummary = new YearlySummaryDTO.MonthSummaryDTO();
            monthSummary.setMonth(budget.getMonth());
            monthSummary.setBudgetId(budget.getId());
            monthSummary.setPlannedIncome(budgetDTO.getTotalPlannedIncome());
            monthSummary.setActualIncome(budgetDTO.getTotalIncome());
            monthSummary.setPlannedExpenses(budgetDTO.getTotalPlannedExpenses());
            monthSummary.setActualExpenses(budgetDTO.getTotalExpenses());
            monthSummary.setPlannedSavings(budgetDTO.getTotalPlannedIncome().subtract(budgetDTO.getTotalPlannedExpenses()));
            monthSummary.setActualSavings(budgetDTO.getTotalIncome().subtract(budgetDTO.getTotalExpenses()));

            months.add(monthSummary);

            totalPlannedIncome = totalPlannedIncome.add(budgetDTO.getTotalPlannedIncome());
            totalActualIncome = totalActualIncome.add(budgetDTO.getTotalIncome());
            totalPlannedExpenses = totalPlannedExpenses.add(budgetDTO.getTotalPlannedExpenses());
            totalActualExpenses = totalActualExpenses.add(budgetDTO.getTotalExpenses());
        }

        summary.setMonths(months);
        summary.setTotalPlannedIncome(totalPlannedIncome);
        summary.setTotalActualIncome(totalActualIncome);
        summary.setTotalPlannedExpenses(totalPlannedExpenses);
        summary.setTotalActualExpenses(totalActualExpenses);
        summary.setTotalPlannedSavings(totalPlannedIncome.subtract(totalPlannedExpenses));
        summary.setTotalActualSavings(totalActualIncome.subtract(totalActualExpenses));

        return summary;
    }

    @Transactional(readOnly = true)
    public Budget getBudgetEntity(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
    }
}
