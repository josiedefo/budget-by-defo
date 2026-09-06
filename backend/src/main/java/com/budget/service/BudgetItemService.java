package com.budget.service;

import com.budget.dto.BudgetItemDTO;
import com.budget.dto.CreateItemRequest;
import com.budget.dto.UpdateItemRequest;
import com.budget.model.Budget;
import com.budget.model.BudgetItem;
import com.budget.model.Section;
import com.budget.repository.BudgetItemRepository;
import com.budget.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class BudgetItemService {

    private final BudgetItemRepository budgetItemRepository;
    private final SectionService sectionService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BudgetItemDTO createItem(CreateItemRequest request) {
        Section section = sectionService.getSectionEntity(request.getSectionId());

        Integer maxOrder = budgetItemRepository.findMaxDisplayOrderBySectionId(request.getSectionId());

        BudgetItem item = new BudgetItem();
        item.setName(request.getName());
        item.setPlannedAmount(request.getPlannedAmount() != null ? request.getPlannedAmount() : BigDecimal.ZERO);
        item.setActualAmount(request.getActualAmount() != null ? request.getActualAmount() : BigDecimal.ZERO);
        item.setDisplayOrder(maxOrder + 1);
        item.setSection(section);

        item = budgetItemRepository.save(item);
        return BudgetItemDTO.fromEntity(item);
    }

    @Transactional
    public BudgetItemDTO updateItem(Long id, UpdateItemRequest request) {
        BudgetItem item = budgetItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found with id: " + id));

        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getPlannedAmount() != null) {
            item.setPlannedAmount(request.getPlannedAmount());
        }
        if (request.getActualAmount() != null) {
            item.setActualAmount(request.getActualAmount());
        }
        if (request.getIsExcludedFromBudget() != null) {
            item.setIsExcludedFromBudget(request.getIsExcludedFromBudget());
        }
        if (request.getIsKeyItem() != null) {
            item.setIsKeyItem(request.getIsKeyItem());
        }

        item = budgetItemRepository.save(item);
        BudgetItemDTO dto = BudgetItemDTO.fromEntity(item);
        populateActualAmount(dto, item);
        return dto;
    }

    /**
     * The persisted actual_amount column is a stale placeholder — the real value is always derived
     * live from transactions (see BudgetService.populateActualAmounts). Without this, a metadata-only
     * update (key-item tag, excluded-from-budget toggle, planned-amount edit) would echo back that
     * stale column value and the frontend would overwrite its correct, computed actual amount with
     * it — resetting the "Actual" column to 0 until the next full page load.
     */
    private void populateActualAmount(BudgetItemDTO dto, BudgetItem item) {
        Section section = item.getSection();
        Budget budget = section.getBudget();
        YearMonth yearMonth = YearMonth.of(budget.getYear(), budget.getMonth());

        BigDecimal actual = transactionRepository.sumAmountByBudgetItemIdAndDateRange(
                item.getId(), yearMonth.atDay(1), yearMonth.atEndOfMonth());
        if (actual == null) {
            actual = BigDecimal.ZERO;
        }
        // For expense sections, negate so spending shows as positive (mirrors populateActualAmounts)
        if (!Boolean.TRUE.equals(section.getIsIncome())) {
            actual = actual.negate();
        }
        dto.setActualAmount(actual);
        dto.setDifference(dto.getPlannedAmount().subtract(actual));
    }

    @Transactional
    public void deleteItem(Long id) {
        if (!budgetItemRepository.existsById(id)) {
            throw new EntityNotFoundException("Budget item not found with id: " + id);
        }
        budgetItemRepository.deleteById(id);
    }
}
