package com.budget.service;

import com.budget.dto.BudgetDTO;
import com.budget.dto.BudgetItemDTO;
import com.budget.dto.ItemInsightDTO;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // ── getItemInsight ──

    @Test
    void getItemInsight_matchesByNameCaseInsensitivelyAcrossMonths_forPastYear() {
        // Two months of a past year (2020) so the "not the current year" branch is exercised
        // deterministically: monthsElapsed = 12, currentMonth = latest month with data.
        Budget juneBudget = singleItemExpenseBudget(2020, 6, 1L, 10L, "Food", 601L, "Groceries", "100.00");
        Budget julyBudget = singleItemExpenseBudget(2020, 7, 2L, 20L, "food", 701L, "GROCERIES", "120.00");

        when(budgetRepository.findByYearOrderByMonthAsc(2020)).thenReturn(List.of(juneBudget, julyBudget));
        when(budgetRepository.findByIdWithSectionsAndItems(1L)).thenReturn(Optional.of(juneBudget));
        when(budgetRepository.findByIdWithSectionsAndItems(2L)).thenReturn(Optional.of(julyBudget));
        when(transactionRepository.sumAmountsByBudgetItemAndDateRange(any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{601L, new BigDecimal("-80.00")},  // -> actual 80.00 in June
                        new Object[]{701L, new BigDecimal("-100.00")} // -> actual 100.00 in July
                ));

        ItemInsightDTO insight = service.getItemInsight(2020, "FOOD", "groceries");

        assertThat(insight.getIsIncome()).isFalse();
        assertThat(insight.getMonths()).hasSize(2);
        assertThat(insight.getMonths().get(0).getMonth()).isEqualTo(6);
        assertThat(insight.getMonths().get(0).getActual()).isEqualByComparingTo("80.00");
        assertThat(insight.getMonths().get(1).getMonth()).isEqualTo(7);
        assertThat(insight.getMonths().get(1).getActual()).isEqualByComparingTo("100.00");

        assertThat(insight.getAnnualPlanned()).isEqualByComparingTo("220.00");
        assertThat(insight.getAnnualActual()).isEqualByComparingTo("180.00");
        // Past year: YTD covers the full year, so it equals the annual total.
        assertThat(insight.getYtdActual()).isEqualByComparingTo("180.00");
        assertThat(insight.getMonthlyAverageActual()).isEqualByComparingTo("15.00"); // 180 / 12

        // "Current month" for a past year is the latest month with data (July); the item was
        // the section's only expense, so its share of that month's spending is 100%.
        assertThat(insight.getCurrentMonth()).isEqualTo(7);
        assertThat(insight.getCurrentMonthActual()).isEqualByComparingTo("100.00");
        assertThat(insight.getCurrentMonthShare()).isEqualByComparingTo("1.0000");
    }

    @Test
    void getItemInsight_sumsDuplicateNamedItemsWithinTheSameMonthAndSection() {
        Budget budget = new Budget(2020, 3);
        budget.setId(5L);
        budget.setSections(new LinkedHashSet<>());

        Section food = new Section();
        food.setId(30L);
        food.setName("Food");
        food.setIsIncome(false);
        food.setDisplayOrder(1);
        food.setBudget(budget);
        food.setItems(new LinkedHashSet<>());

        BudgetItem groceries1 = expenseItem(301L, "Groceries", "60.00", food);
        BudgetItem groceries2 = expenseItem(302L, "Groceries", "40.00", food);
        food.getItems().add(groceries1);
        food.getItems().add(groceries2);
        budget.getSections().add(food);

        when(budgetRepository.findByYearOrderByMonthAsc(2020)).thenReturn(List.of(budget));
        when(budgetRepository.findByIdWithSectionsAndItems(5L)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountsByBudgetItemAndDateRange(any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{301L, new BigDecimal("-25.00")},
                        new Object[]{302L, new BigDecimal("-15.00")}
                ));

        ItemInsightDTO insight = service.getItemInsight(2020, "Food", "Groceries");

        assertThat(insight.getMonths()).hasSize(1);
        assertThat(insight.getMonths().get(0).getPlanned()).isEqualByComparingTo("100.00");
        assertThat(insight.getMonths().get(0).getActual()).isEqualByComparingTo("40.00");
    }

    @Test
    void getItemInsight_itemNotFoundInAnyMonth_returnsEmptyZeroedResult() {
        Budget budget = singleItemExpenseBudget(2020, 1, 9L, 90L, "Food", 901L, "Groceries", "50.00");

        when(budgetRepository.findByYearOrderByMonthAsc(2020)).thenReturn(List.of(budget));
        when(budgetRepository.findByIdWithSectionsAndItems(9L)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountsByBudgetItemAndDateRange(any(), any()))
                .thenReturn(new ArrayList<>());

        ItemInsightDTO insight = service.getItemInsight(2020, "Food", "Restaurants");

        assertThat(insight.getMonths()).isEmpty();
        assertThat(insight.getIsIncome()).isNull();
        assertThat(insight.getAnnualPlanned()).isEqualByComparingTo("0");
        assertThat(insight.getAnnualActual()).isEqualByComparingTo("0");
        assertThat(insight.getYtdActual()).isEqualByComparingTo("0");
        assertThat(insight.getMonthlyAverageActual()).isEqualByComparingTo("0");
        assertThat(insight.getCurrentMonth()).isNull();
        assertThat(insight.getCurrentMonthActual()).isEqualByComparingTo("0");
        assertThat(insight.getCurrentMonthShare()).isEqualByComparingTo("0");
    }

    @Test
    void getItemInsight_currentYear_computesYtdAverageAndShareThroughToday() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonthValue = today.getMonthValue();

        List<Budget> budgets = new ArrayList<>();
        List<Object[]> sums = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            long budgetId = 1000L + month;
            long itemId = 2000L + month;
            Budget budget = singleItemExpenseBudget(
                    currentYear, month, budgetId, 40L, "Utilities", itemId, "Electric", "100.00");
            budgets.add(budget);
            when(budgetRepository.findByIdWithSectionsAndItems(budgetId)).thenReturn(Optional.of(budget));
            sums.add(new Object[]{itemId, new BigDecimal("-50.00")}); // -> actual 50.00 every month
        }
        when(budgetRepository.findByYearOrderByMonthAsc(currentYear)).thenReturn(budgets);
        when(transactionRepository.sumAmountsByBudgetItemAndDateRange(any(), any())).thenReturn(sums);

        ItemInsightDTO insight = service.getItemInsight(currentYear, "Utilities", "Electric");

        assertThat(insight.getMonths()).hasSize(12);
        assertThat(insight.getAnnualActual()).isEqualByComparingTo("600.00"); // 50 * 12

        BigDecimal expectedYtd = BigDecimal.valueOf(50L * currentMonthValue).setScale(2);
        assertThat(insight.getYtdActual()).isEqualByComparingTo(expectedYtd);

        BigDecimal expectedAverage = new BigDecimal("600.00")
                .divide(BigDecimal.valueOf(currentMonthValue), 2, RoundingMode.HALF_UP);
        assertThat(insight.getMonthlyAverageActual()).isEqualByComparingTo(expectedAverage);

        assertThat(insight.getCurrentMonth()).isEqualTo(currentMonthValue);
        assertThat(insight.getCurrentMonthActual()).isEqualByComparingTo("50.00");
        // Electric is the only expense item in its month, so it is 100% of that month's spending.
        assertThat(insight.getCurrentMonthShare()).isEqualByComparingTo("1.0000");
    }

    private BudgetItem expenseItem(long id, String name, String planned, Section section) {
        BudgetItem item = new BudgetItem();
        item.setId(id);
        item.setName(name);
        item.setPlannedAmount(new BigDecimal(planned));
        item.setActualAmount(BigDecimal.ZERO);
        item.setDisplayOrder(1);
        item.setIsExcludedFromBudget(false);
        item.setIsKeyItem(false);
        item.setSection(section);
        return item;
    }

    private Budget singleItemExpenseBudget(int year, int month, long budgetId, long sectionId,
                                            String sectionName, long itemId, String itemName, String planned) {
        Budget budget = new Budget(year, month);
        budget.setId(budgetId);
        budget.setSections(new LinkedHashSet<>());

        Section section = new Section();
        section.setId(sectionId);
        section.setName(sectionName);
        section.setIsIncome(false);
        section.setDisplayOrder(1);
        section.setBudget(budget);
        section.setItems(new LinkedHashSet<>());

        BudgetItem item = expenseItem(itemId, itemName, planned, section);
        section.getItems().add(item);
        budget.getSections().add(section);
        return budget;
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
