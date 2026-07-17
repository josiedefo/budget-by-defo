package com.budget.service;

import com.budget.dto.BudgetDTO;
import com.budget.dto.BudgetItemDTO;
import com.budget.dto.SectionDTO;
import com.budget.model.Budget;
import com.budget.model.BudgetItem;
import com.budget.model.Section;
import com.budget.repository.BudgetRepository;
import com.budget.repository.PlanRepository;
import com.budget.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private PlanRepository planRepository;

    @InjectMocks private BudgetService service;

    // ── Bug: invalid months used to reach YearMonth.of and blow up with a 500 ──

    @Test
    void getBudget_invalidMonth_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.getBudget(2026, 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid month");
        assertThatThrownBy(() -> service.getBudget(2026, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid month");
        assertThatThrownBy(() -> service.getBudget(20260, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid year");
    }

    @Test
    void copyBudget_invalidTargetMonth_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.copyBudget(2026, 1, 2026, 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid month");
    }

    @Test
    void copyBudget_sameSourceAndTarget_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.copyBudget(2026, 7, 2026, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be the same");
    }

    // ── Bug: viewing a month used to persist computed actuals onto the entities ──

    @Test
    void getBudget_populatesActualsOnDtoWithoutTouchingEntities() {
        Budget budget = budgetWithIncomeAndExpenseSections();
        when(budgetRepository.findByYearAndMonthWithSectionsAndItems(2026, 7))
                .thenReturn(Optional.of(budget));
        // Signed sums: income item earned 500; expense item spent 200 (negative by convention)
        when(transactionRepository.sumAmountsByBudgetItemAndDateRange(any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{101L, new BigDecimal("500.00")},
                        new Object[]{201L, new BigDecimal("-200.00")}));
        when(planRepository.findByBudgetItemIdsAndYearAndMonth(anyList(), any(), any()))
                .thenReturn(List.of());

        BudgetDTO dto = service.getBudget(2026, 7);

        SectionDTO incomeSection = dto.getSections().stream()
                .filter(SectionDTO::getIsIncome).findFirst().orElseThrow();
        SectionDTO expenseSection = dto.getSections().stream()
                .filter(s -> !s.getIsIncome()).findFirst().orElseThrow();

        BudgetItemDTO incomeItem = incomeSection.getItems().get(0);
        BudgetItemDTO expenseItem = expenseSection.getItems().get(0);

        assertThat(incomeItem.getActualAmount()).isEqualByComparingTo("500.00");
        // Expense sums are negated so spending shows as positive
        assertThat(expenseItem.getActualAmount()).isEqualByComparingTo("200.00");
        assertThat(expenseItem.getDifference()).isEqualByComparingTo("50.00"); // 250 planned - 200 actual

        // Totals recomputed from the actuals
        assertThat(dto.getTotalIncome()).isEqualByComparingTo("500.00");
        assertThat(dto.getTotalExpenses()).isEqualByComparingTo("200.00");

        // The managed entities must NOT be mutated — that's what used to leak into the DB
        budget.getSections().forEach(section ->
                section.getItems().forEach(item ->
                        assertThat(item.getActualAmount()).isEqualByComparingTo("0.00")));
    }

    @Test
    void getBudget_missingBudget_returnsNull() {
        when(budgetRepository.findByYearAndMonthWithSectionsAndItems(2026, 7))
                .thenReturn(Optional.empty());

        assertThat(service.getBudget(2026, 7)).isNull();
    }

    private Budget budgetWithIncomeAndExpenseSections() {
        Budget budget = new Budget(2026, 7);
        budget.setId(1L);
        budget.setSections(new LinkedHashSet<>());

        Section income = new Section();
        income.setId(10L);
        income.setName("Income");
        income.setIsIncome(true);
        income.setDisplayOrder(1);
        income.setBudget(budget);
        income.setItems(new LinkedHashSet<>());
        BudgetItem salary = new BudgetItem();
        salary.setId(101L);
        salary.setName("Salary");
        salary.setPlannedAmount(new BigDecimal("600.00"));
        salary.setActualAmount(BigDecimal.ZERO);
        salary.setDisplayOrder(1);
        salary.setIsExcludedFromBudget(false);
        salary.setIsKeyItem(false);
        salary.setSection(income);
        income.getItems().add(salary);

        Section food = new Section();
        food.setId(20L);
        food.setName("Food");
        food.setIsIncome(false);
        food.setDisplayOrder(2);
        food.setBudget(budget);
        food.setItems(new LinkedHashSet<>());
        BudgetItem groceries = new BudgetItem();
        groceries.setId(201L);
        groceries.setName("Groceries");
        groceries.setPlannedAmount(new BigDecimal("250.00"));
        groceries.setActualAmount(BigDecimal.ZERO);
        groceries.setDisplayOrder(1);
        groceries.setIsExcludedFromBudget(false);
        groceries.setIsKeyItem(false);
        groceries.setSection(food);
        food.getItems().add(groceries);

        budget.getSections().add(income);
        budget.getSections().add(food);
        return budget;
    }
}
