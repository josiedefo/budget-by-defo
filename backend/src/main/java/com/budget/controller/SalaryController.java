package com.budget.controller;

import com.budget.dto.CreateSalaryRequest;
import com.budget.dto.SalaryDTO;
import com.budget.dto.UpdateSalaryRequest;
import com.budget.service.SalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    @GetMapping
    public List<SalaryDTO> getAllSalaries() {
        return salaryService.getAllSalaries();
    }

    @GetMapping("/{id}")
    public SalaryDTO getSalary(@PathVariable Long id) {
        return salaryService.getSalary(id);
    }

    @PostMapping
    public SalaryDTO createSalary(@Valid @RequestBody CreateSalaryRequest request) {
        return salaryService.createSalary(request);
    }

    @PutMapping("/{id}")
    public SalaryDTO updateSalary(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalaryRequest request) {
        return salaryService.updateSalary(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalary(@PathVariable Long id) {
        salaryService.deleteSalary(id);
        return ResponseEntity.noContent().build();
    }
}
