package com.budget.controller;

import com.budget.dto.BudgetDTO;
import com.budget.dto.CopyBudgetRequest;
import com.budget.dto.YearlySummaryDTO;
import com.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/{year}/{month}")
    public ResponseEntity<BudgetDTO> getBudget(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestParam(defaultValue = "true") boolean createIfMissing) {

        BudgetDTO budget;
        if (createIfMissing) {
            budget = budgetService.getOrCreateBudget(year, month);
        } else {
            budget = budgetService.getBudget(year, month);
            if (budget == null) {
                return ResponseEntity.notFound().build();
            }
        }

        return ResponseEntity.ok(budget);
    }

    @GetMapping("/{year}")
    public ResponseEntity<YearlySummaryDTO> getYearlySummary(@PathVariable Integer year) {
        YearlySummaryDTO summary = budgetService.getYearlySummary(year);
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        BudgetDTO budget = budgetService.createBudget(year, month);
        return ResponseEntity.ok(budget);
    }

    @PostMapping("/{year}/{month}/copy")
    public ResponseEntity<BudgetDTO> copyBudget(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @Valid @RequestBody CopyBudgetRequest request) {
        BudgetDTO budget = budgetService.copyBudget(
                request.getSourceYear(), request.getSourceMonth(), year, month);
        return ResponseEntity.ok(budget);
    }
}
