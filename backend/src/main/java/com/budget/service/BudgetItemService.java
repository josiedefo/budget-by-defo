package com.budget.service;

import com.budget.dto.BudgetItemDTO;
import com.budget.dto.CreateItemRequest;
import com.budget.dto.UpdateItemRequest;
import com.budget.model.BudgetItem;
import com.budget.model.Section;
import com.budget.repository.BudgetItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BudgetItemService {

    private final BudgetItemRepository budgetItemRepository;
    private final SectionService sectionService;

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

        item = budgetItemRepository.save(item);
        return BudgetItemDTO.fromEntity(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        if (!budgetItemRepository.existsById(id)) {
            throw new EntityNotFoundException("Budget item not found with id: " + id);
        }
        budgetItemRepository.deleteById(id);
    }
}
