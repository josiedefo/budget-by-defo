package com.budget.controller;

import com.budget.dto.CreateSavingsFundRequest;
import com.budget.dto.SavingsFundDTO;
import com.budget.dto.SavingsSummaryDTO;
import com.budget.dto.UpdateSavingsFundRequest;
import com.budget.service.SavingsFundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings/funds")
@RequiredArgsConstructor
public class SavingsFundController {

    private final SavingsFundService savingsFundService;

    @GetMapping
    public List<SavingsFundDTO> getAllFunds() {
        return savingsFundService.getAllFunds();
    }

    @GetMapping("/{id}")
    public SavingsFundDTO getFund(@PathVariable Long id) {
        return savingsFundService.getFund(id);
    }

    @PostMapping
    public SavingsFundDTO createFund(@Valid @RequestBody CreateSavingsFundRequest request) {
        return savingsFundService.createFund(request);
    }

    @PutMapping("/{id}")
    public SavingsFundDTO updateFund(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSavingsFundRequest request) {
        return savingsFundService.updateFund(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFund(@PathVariable Long id) {
        savingsFundService.deleteFund(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public SavingsSummaryDTO getYearSummary() {
        return savingsFundService.getYearSummary();
    }
}
