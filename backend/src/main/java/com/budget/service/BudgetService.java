package com.budget.service;

import com.budget.dto.BudgetDTO;
import com.budget.dto.BudgetItemDTO;
import com.budget.dto.SectionDTO;
import com.budget.dto.YearlySummaryDTO;
import com.budget.model.Budget;
import com.budget.model.BudgetItem;
import com.budget.model.Plan;
import com.budget.model.PlanItem;
import com.budget.model.Section;
import com.budget.repository.BudgetRepository;
import com.budget.repository.PlanRepository;
import com.budget.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final PlanRepository planRepository;

    // Section name -> (isIncome, items[])
    private static final Map<String, SectionConfig> DEFAULT_SECTIONS = new LinkedHashMap<>();

    static {
        DEFAULT_SECTIONS.put("Income", new SectionConfig(true,
            "Josie Salary", "Patrick Salary"));

        DEFAULT_SECTIONS.put("Savings", new SectionConfig(false,
            "Njangui Savings", "Patrick Roth IRA", "Josie Roth IRA",
            "Joshua College Fund", "Joy College Fund", "Extra Savings"));

        DEFAULT_SECTIONS.put("House Bills", new SectionConfig(false,
            "Mortgage", "Electric/Power", "Water/Sewer/Trash", "Mobile Phone",
             "HOA", "House Supplies/Furnishings/Appliances"));

        DEFAULT_SECTIONS.put("Daily Living", new SectionConfig(false,
            "Groceries", "Restaurants", "Patrick Allowance", "Josie Allowance",
            "Clothing", "Hair", "Cosmetics", "Amusement"));

        DEFAULT_SECTIONS.put("Giving", new SectionConfig(false,
            "Tithe", "Gifts"));

        DEFAULT_SECTIONS.put("Transportation", new SectionConfig(false,
            "Gas & Public Bus", "Services/Repairs/Parts"));

        DEFAULT_SECTIONS.put("Children", new SectionConfig(false,
            "Kids Supplies", "Kids Activities"));

        DEFAULT_SECTIONS.put("Education", new SectionConfig(false,
            "Tuition", "Books & Supplies"));

        DEFAULT_SECTIONS.put("Vacation", new SectionConfig(false,
            "Vacation", "Airfare Travel", "Car Travel"));

        DEFAULT_SECTIONS.put("Insurance", new SectionConfig(false,
            "Life Insurance"));

        DEFAULT_SECTIONS.put("Misc", new SectionConfig(false,
            "Transfer", "Interest Payment"));
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
        validateYearMonth(year, month);
        Budget budget = budgetRepository.findByYearAndMonthWithSectionsAndItems(year, month)
                .orElse(null);

        if (budget == null) {
            return null;
        }

        BudgetDTO dto = BudgetDTO.fromEntity(budget);
        populateActualAmounts(dto, year, month);
        populatePlanIds(dto, year, month);
        return dto;
    }

    @Transactional
    public BudgetDTO getOrCreateBudget(Integer year, Integer month) {
        validateYearMonth(year, month);
        Budget budget = budgetRepository.findByYearAndMonthWithSectionsAndItems(year, month)
                .orElseGet(() -> createBudgetWithDefaults(year, month));

        BudgetDTO dto = BudgetDTO.fromEntity(budget);
        populateActualAmounts(dto, year, month);
        populatePlanIds(dto, year, month);
        return dto;
    }

    /** Rejects impossible year/month values with a 400 instead of a 500 from YearMonth.of. */
    private static void validateYearMonth(Integer year, Integer month) {
        if (year == null || year < 1900 || year > 2200) {
            throw new IllegalArgumentException("Invalid year: " + year);
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
    }

    private void populatePlanIds(BudgetDTO budgetDTO, Integer year, Integer month) {
        // Collect all budget item IDs
        List<Long> budgetItemIds = budgetDTO.getSections().stream()
                .flatMap(s -> s.getItems().stream())
                .map(BudgetItemDTO::getId)
                .collect(java.util.stream.Collectors.toList());

        if (budgetItemIds.isEmpty()) {
            return;
        }

        // Fetch all plans for these budget items in this month
        List<Plan> plans = planRepository.findByBudgetItemIdsAndYearAndMonth(budgetItemIds, year, month);

        // Build map of budgetItemId -> planId
        Map<Long, Long> planIdMap = new HashMap<>();
        for (Plan plan : plans) {
            planIdMap.put(plan.getBudgetItem().getId(), plan.getId());
        }

        // Populate planId on each item
        for (SectionDTO section : budgetDTO.getSections()) {
            for (BudgetItemDTO item : section.getItems()) {
                item.setPlanId(planIdMap.get(item.getId()));
            }
        }
    }

    /**
     * Computes actual amounts from transactions onto the DTO (never onto managed entities —
     * mutating entities inside a writable transaction would flush the computed values into
     * budget_item.actual_amount as a side effect of viewing the month).
     */
    private void populateActualAmounts(BudgetDTO budgetDTO, Integer year, Integer month) {
        // Get date range for the month
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Get transaction sums grouped by budget item ID
        List<Object[]> transactionSums = transactionRepository.sumAmountsByBudgetItemAndDateRange(startDate, endDate);

        // Build a map of budgetItemId -> signed sum (income positive, expense negative)
        Map<Long, BigDecimal> actualAmounts = new HashMap<>();
        for (Object[] row : transactionSums) {
            Long itemId = (Long) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            actualAmounts.put(itemId, sum);
        }

        // Update each item DTO's actual amount by matching ID
        for (SectionDTO section : budgetDTO.getSections()) {
            for (BudgetItemDTO item : section.getItems()) {
                BigDecimal actualAmount = actualAmounts.getOrDefault(item.getId(), BigDecimal.ZERO);
                // For expense sections, negate so spending shows as positive
                if (!Boolean.TRUE.equals(section.getIsIncome())) {
                    actualAmount = actualAmount.negate();
                }
                item.setActualAmount(actualAmount);
                item.setDifference(item.getPlannedAmount().subtract(actualAmount));
            }
        }

        budgetDTO.recomputeTotals();
    }

    @Transactional
    public BudgetDTO createBudget(Integer year, Integer month) {
        validateYearMonth(year, month);
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
            populateActualAmounts(budgetDTO, year, budget.getMonth());

            YearlySummaryDTO.MonthSummaryDTO monthSummary = new YearlySummaryDTO.MonthSummaryDTO();
            monthSummary.setMonth(budget.getMonth());
            monthSummary.setBudgetId(budget.getId());
            monthSummary.setPlannedIncome(budgetDTO.getTotalPlannedIncome());
            monthSummary.setActualIncome(budgetDTO.getTotalIncome());
            monthSummary.setPlannedExpenses(budgetDTO.getTotalPlannedExpenses());
            monthSummary.setActualExpenses(budgetDTO.getTotalExpenses());
            monthSummary.setPlannedSavings(budgetDTO.getTotalPlannedIncome().subtract(budgetDTO.getTotalPlannedExpenses()));
            monthSummary.setActualSavings(budgetDTO.getTotalIncome().subtract(budgetDTO.getTotalExpenses()));

            List<YearlySummaryDTO.KeyItemDTO> keyItems = new ArrayList<>();
            for (SectionDTO section : budgetDTO.getSections()) {
                for (BudgetItemDTO item : section.getItems()) {
                    if (Boolean.TRUE.equals(item.getIsKeyItem())) {
                        keyItems.add(new YearlySummaryDTO.KeyItemDTO(
                                item.getName(),
                                section.getName(),
                                item.getActualAmount().subtract(item.getPlannedAmount()),
                                section.getIsIncome()
                        ));
                    }
                }
            }
            monthSummary.setKeyItems(keyItems);

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

    @Transactional
    public BudgetDTO copyBudget(Integer sourceYear, Integer sourceMonth,
                                Integer targetYear, Integer targetMonth) {
        validateYearMonth(sourceYear, sourceMonth);
        validateYearMonth(targetYear, targetMonth);
        // Guard: source and target cannot be the same month
        if (sourceYear.equals(targetYear) && sourceMonth.equals(targetMonth)) {
            throw new IllegalArgumentException("Source and target month cannot be the same");
        }

        // Load source budget — 404 if not found
        Budget source = budgetRepository.findByYearAndMonthWithSectionsAndItems(sourceYear, sourceMonth)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No budget found for " + sourceYear + "/" + sourceMonth));

        // Resolve or create target budget
        Budget target = budgetRepository.findByYearAndMonth(targetYear, targetMonth)
                .orElseGet(() -> {
                    Budget b = new Budget(targetYear, targetMonth);
                    b.setSections(new LinkedHashSet<>());
                    return budgetRepository.save(b);
                });

        if (target.getSections().isEmpty()) {
            // Full copy: target is new/empty — create all sections and items from source
            for (Section srcSection : source.getSections()) {
                Section newSection = new Section();
                newSection.setName(srcSection.getName());
                newSection.setDisplayOrder(srcSection.getDisplayOrder());
                newSection.setIsIncome(srcSection.getIsIncome());
                newSection.setBudget(target);
                newSection.setItems(new LinkedHashSet<>());

                // Items lazy-load safely within @Transactional via @BatchSize(25)
                for (BudgetItem srcItem : srcSection.getItems()) {
                    BudgetItem newItem = new BudgetItem();
                    newItem.setName(srcItem.getName());
                    newItem.setDisplayOrder(srcItem.getDisplayOrder());
                    newItem.setPlannedAmount(srcItem.getPlannedAmount());
                    newItem.setActualAmount(BigDecimal.ZERO); // never copy actual amounts
                    newItem.setIsExcludedFromBudget(srcItem.getIsExcludedFromBudget());
                    newItem.setSection(newSection);
                    newSection.getItems().add(newItem);
                }

                target.getSections().add(newSection);
            }
        } else {
            // Merge copy: target already has sections — match by name and update planned amounts only.
            // This preserves existing item IDs so transaction links remain intact.
            Map<String, Section> targetSectionsByName = new HashMap<>();
            for (Section s : target.getSections()) {
                targetSectionsByName.put(s.getName().toLowerCase(), s);
            }

            for (Section srcSection : source.getSections()) {
                Section targetSection = targetSectionsByName.get(srcSection.getName().toLowerCase());
                if (targetSection != null) {
                    // Update section metadata
                    targetSection.setDisplayOrder(srcSection.getDisplayOrder());
                    targetSection.setIsIncome(srcSection.getIsIncome());

                    // Build item lookup for this target section
                    Map<String, BudgetItem> targetItemsByName = new HashMap<>();
                    for (BudgetItem item : targetSection.getItems()) {
                        targetItemsByName.put(item.getName().toLowerCase(), item);
                    }

                    // Match source items to target items by name; preserve IDs for transaction link integrity
                    for (BudgetItem srcItem : srcSection.getItems()) {
                        BudgetItem targetItem = targetItemsByName.get(srcItem.getName().toLowerCase());
                        if (targetItem != null) {
                            // Preserve existing item ID — just update planned amount and metadata
                            targetItem.setPlannedAmount(srcItem.getPlannedAmount());
                            targetItem.setDisplayOrder(srcItem.getDisplayOrder());
                            targetItem.setIsExcludedFromBudget(srcItem.getIsExcludedFromBudget());
                        } else {
                            // New item in source not found in target — add it
                            BudgetItem newItem = new BudgetItem();
                            newItem.setName(srcItem.getName());
                            newItem.setDisplayOrder(srcItem.getDisplayOrder());
                            newItem.setPlannedAmount(srcItem.getPlannedAmount());
                            newItem.setActualAmount(BigDecimal.ZERO);
                            newItem.setIsExcludedFromBudget(srcItem.getIsExcludedFromBudget());
                            newItem.setSection(targetSection);
                            targetSection.getItems().add(newItem);
                        }
                    }
                } else {
                    // Source section not found in target — create it with all its items
                    Section newSection = new Section();
                    newSection.setName(srcSection.getName());
                    newSection.setDisplayOrder(srcSection.getDisplayOrder());
                    newSection.setIsIncome(srcSection.getIsIncome());
                    newSection.setBudget(target);
                    newSection.setItems(new LinkedHashSet<>());

                    for (BudgetItem srcItem : srcSection.getItems()) {
                        BudgetItem newItem = new BudgetItem();
                        newItem.setName(srcItem.getName());
                        newItem.setDisplayOrder(srcItem.getDisplayOrder());
                        newItem.setPlannedAmount(srcItem.getPlannedAmount());
                        newItem.setActualAmount(BigDecimal.ZERO);
                        newItem.setIsExcludedFromBudget(srcItem.getIsExcludedFromBudget());
                        newItem.setSection(newSection);
                        newSection.getItems().add(newItem);
                    }

                    target.getSections().add(newSection);
                }
            }
        }

        target = budgetRepository.save(target);
        budgetRepository.flush(); // ensure new item IDs are visible before plan copy
        copyPlans(source, target, sourceYear, sourceMonth, targetYear, targetMonth);
        BudgetDTO dto = BudgetDTO.fromEntity(target);
        populateActualAmounts(dto, targetYear, targetMonth);
        populatePlanIds(dto, targetYear, targetMonth);
        return dto;
    }

    /**
     * For every source budget item that has a Plan in the source month, creates (or replaces)
     * a matching Plan in the target month, linked to the corresponding target budget item.
     * Matching is done by section name + item name (case-insensitive), consistent with the
     * merge-copy strategy used for budget items themselves.
     */
    private void copyPlans(Budget source, Budget target,
                           Integer sourceYear, Integer sourceMonth,
                           Integer targetYear, Integer targetMonth) {
        // Collect all source item IDs and build lookup maps from the in-memory graph
        Map<Long, String> sourceItemSectionName = new HashMap<>();
        Map<Long, String> sourceItemName = new HashMap<>();
        for (Section section : source.getSections()) {
            for (BudgetItem item : section.getItems()) {
                sourceItemSectionName.put(item.getId(), section.getName());
                sourceItemName.put(item.getId(), item.getName());
            }
        }

        List<Long> sourceItemIds = new ArrayList<>(sourceItemSectionName.keySet());
        if (sourceItemIds.isEmpty()) return;

        // Load plans for the source month
        List<Plan> sourcePlans = planRepository.findByBudgetItemIdsAndYearAndMonth(
                sourceItemIds, sourceYear, sourceMonth);
        if (sourcePlans.isEmpty()) return;

        // Build target item lookup: "sectionName|itemName" -> BudgetItem
        Map<String, BudgetItem> targetItemByKey = new HashMap<>();
        for (Section section : target.getSections()) {
            for (BudgetItem item : section.getItems()) {
                String key = section.getName().toLowerCase() + "|" + item.getName().toLowerCase();
                targetItemByKey.put(key, item);
            }
        }

        for (Plan srcPlan : sourcePlans) {
            Long srcItemId = srcPlan.getBudgetItem().getId();
            String sectionName = sourceItemSectionName.get(srcItemId);
            String itemName = sourceItemName.get(srcItemId);
            if (sectionName == null || itemName == null) continue;

            String key = sectionName.toLowerCase() + "|" + itemName.toLowerCase();
            BudgetItem targetItem = targetItemByKey.get(key);
            if (targetItem == null) continue;

            // Upsert: if a plan already exists for this target item+month, delete it first
            planRepository.findByBudgetItemIdAndYearAndMonth(targetItem.getId(), targetYear, targetMonth)
                    .ifPresent(existing -> {
                        planRepository.delete(existing);
                        planRepository.flush();
                    });

            // Create the new plan, copying all items from the source plan
            Plan newPlan = new Plan();
            newPlan.setBudgetItem(targetItem);
            newPlan.setYear(targetYear);
            newPlan.setMonth(targetMonth);
            newPlan.setItems(new LinkedHashSet<>());

            for (PlanItem srcPlanItem : srcPlan.getItems()) {
                PlanItem newPlanItem = new PlanItem();
                newPlanItem.setPlan(newPlan);
                newPlanItem.setName(srcPlanItem.getName());
                newPlanItem.setAmount(srcPlanItem.getAmount());
                newPlanItem.setDisplayOrder(srcPlanItem.getDisplayOrder());
                newPlanItem.setFromSubscription(srcPlanItem.getFromSubscription());
                newPlanItem.setFromSalary(srcPlanItem.getFromSalary());
                newPlan.getItems().add(newPlanItem);
            }

            planRepository.save(newPlan);
        }
    }

    @Transactional(readOnly = true)
    public Budget getBudgetEntity(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
    }

    @Transactional
    public Budget getOrCreateBudgetEntity(Integer year, Integer month) {
        validateYearMonth(year, month);
        return budgetRepository.findByYearAndMonth(year, month)
                .orElseGet(() -> createBudgetWithDefaults(year, month));
    }
}
