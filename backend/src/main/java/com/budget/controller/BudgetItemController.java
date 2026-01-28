package com.budget.controller;

import com.budget.dto.BudgetItemDTO;
import com.budget.dto.CreateItemRequest;
import com.budget.dto.UpdateItemRequest;
import com.budget.service.BudgetItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class BudgetItemController {

    private final BudgetItemService budgetItemService;

    @PostMapping
    public ResponseEntity<BudgetItemDTO> createItem(@Valid @RequestBody CreateItemRequest request) {
        BudgetItemDTO item = budgetItemService.createItem(request);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetItemDTO> updateItem(
            @PathVariable Long id,
            @RequestBody UpdateItemRequest request) {
        BudgetItemDTO item = budgetItemService.updateItem(id, request);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        budgetItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
