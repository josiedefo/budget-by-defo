package com.budget.controller;

import com.budget.dto.CreatePlanRequest;
import com.budget.dto.PlanDTO;
import com.budget.dto.UpdatePlanRequest;
import com.budget.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanDTO>> getPlans(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(planService.getPlansForMonth(year, month));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanDTO> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlan(id));
    }

    @GetMapping("/by-item")
    public ResponseEntity<PlanDTO> getPlanByBudgetItem(
            @RequestParam Long budgetItemId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        PlanDTO plan = planService.getPlanByBudgetItem(budgetItemId, year, month);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }

    @PostMapping
    public ResponseEntity<PlanDTO> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(planService.createPlan(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanDTO> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
