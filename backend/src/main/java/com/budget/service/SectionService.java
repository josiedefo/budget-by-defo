package com.budget.service;

import com.budget.dto.CreateSectionRequest;
import com.budget.dto.SectionDTO;
import com.budget.dto.UpdateSectionRequest;
import com.budget.model.Budget;
import com.budget.model.Section;
import com.budget.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final BudgetService budgetService;

    @Transactional
    public SectionDTO createSection(CreateSectionRequest request) {
        Budget budget = budgetService.getBudgetEntity(request.getBudgetId());

        Integer maxOrder = sectionRepository.findMaxDisplayOrderByBudgetId(request.getBudgetId());

        Section section = new Section();
        section.setName(request.getName());
        section.setIsIncome(request.getIsIncome() != null ? request.getIsIncome() : false);
        section.setDisplayOrder(maxOrder + 1);
        section.setBudget(budget);
        section.setItems(new ArrayList<>());

        section = sectionRepository.save(section);
        return SectionDTO.fromEntity(section);
    }

    @Transactional
    public SectionDTO updateSection(Long id, UpdateSectionRequest request) {
        Section section = sectionRepository.findByIdWithItems(id)
                .orElseThrow(() -> new EntityNotFoundException("Section not found with id: " + id));

        if (request.getName() != null) {
            section.setName(request.getName());
        }
        if (request.getIsIncome() != null) {
            section.setIsIncome(request.getIsIncome());
        }

        section = sectionRepository.save(section);
        return SectionDTO.fromEntity(section);
    }

    @Transactional
    public void deleteSection(Long id) {
        if (!sectionRepository.existsById(id)) {
            throw new EntityNotFoundException("Section not found with id: " + id);
        }
        sectionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Section getSectionEntity(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Section not found with id: " + id));
    }
}
