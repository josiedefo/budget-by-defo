package com.budget.service;

import com.budget.dto.BudgetDTO;
import com.budget.dto.YearlySummaryDTO;
import com.budget.model.Budget;
import com.budget.model.BudgetItem;
import com.budget.model.Section;
import com.budget.repository.BudgetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;

    // Section name -> (isIncome, items[])
    private static final Map<String, SectionConfig> DEFAULT_SECTIONS = new LinkedHashMap<>();

    static {
        DEFAULT_SECTIONS.put("Income", new SectionConfig(true,
            "Josie Salary", "Patrick Salary", "From Extra Savings", "From Vacation Savings",
            "From Kids School Savings", "From Student Loan Savings", "From Njangui Savings",
            "From Emergency Savings", "Refunds/Reimbursements", "Pat&Js LLC", "Taxes"));

        DEFAULT_SECTIONS.put("Savings", new SectionConfig(false,
            "Njangui Savings", "Patrick Roth IRA", "Josie Roth IRA",
            "Joshua College Fund", "Joy College Fund", "Extra Savings"));

        DEFAULT_SECTIONS.put("House", new SectionConfig(false,
            "Mortgage", "Electric/Power", "Water/Sewer/Trash", "Mobile Phone",
            "Internet", "HOA", "House Supplies/Furnishings/Appliances", "Home Services"));

        DEFAULT_SECTIONS.put("Daily Living", new SectionConfig(false,
            "Groceries", "Restaurants", "Patrick Allowance", "Josie Allowance",
            "Grandma Expenses", "Clothing", "Hair", "Cosmetics", "Amusement",
            "Sport", "Cameroon Fund", "Pat&Js LLC"));

        DEFAULT_SECTIONS.put("Giving", new SectionConfig(false,
            "Tithe", "Gifts"));

        DEFAULT_SECTIONS.put("Transportation", new SectionConfig(false,
            "Gas & Public Bus", "Services/Repairs/Parts", "Auto Insurance",
            "Registration/License Renewal", "Tolls", "Traffic Ticket"));

        DEFAULT_SECTIONS.put("Children", new SectionConfig(false,
            "Kids Supplies", "Kids Activities", "School"));

        DEFAULT_SECTIONS.put("Education", new SectionConfig(false,
            "Tuition", "Student Loan", "Books & Supplies"));

        DEFAULT_SECTIONS.put("Vacation", new SectionConfig(false,
            "Vacation", "Airfare Travel", "Car Travel"));

        DEFAULT_SECTIONS.put("Insurance", new SectionConfig(false,
            "Life Insurance"));

        DEFAULT_SECTIONS.put("Misc", new SectionConfig(false,
            "Transfer", "Interest Payment", "Filing Taxes", "Federal Taxes",
            "State/Local Taxes", "Bank Fees", "Echange avec autrui"));
    }

    private static class SectionConfig {
        final boolean isIncome;
        final String[] items;

        SectionConfig(boolean isIncome, String... items) {
            this.isIncome = isIncome;
            this.items = items;
        }
    }

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
        budget.setSections(new LinkedHashSet<>());

        int sectionOrder = 1;
        for (Map.Entry<String, SectionConfig> entry : DEFAULT_SECTIONS.entrySet()) {
            String sectionName = entry.getKey();
            SectionConfig config = entry.getValue();

            Section section = new Section();
            section.setName(sectionName);
            section.setDisplayOrder(sectionOrder++);
            section.setIsIncome(config.isIncome);
            section.setBudget(budget);
            section.setItems(new LinkedHashSet<>());

            int itemOrder = 1;
            for (String itemName : config.items) {
                BudgetItem item = new BudgetItem();
                item.setName(itemName);
                item.setPlannedAmount(BigDecimal.ZERO);
                item.setActualAmount(BigDecimal.ZERO);
                item.setDisplayOrder(itemOrder++);
                item.setSection(section);
                section.getItems().add(item);
            }

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
