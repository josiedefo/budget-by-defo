package com.budget.controller;

import com.budget.dto.BudgetItemLinkStatus;
import com.budget.service.SavingsLinkStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savings/link-status")
@RequiredArgsConstructor
public class SavingsLinkStatusController {

    private final SavingsLinkStatusService savingsLinkStatusService;

    /**
     * Returns savings link status for a batch of budget items in a date range.
     * GET /api/savings/link-status/budget-items?ids=1,2,3&startDate=2026-06-01&endDate=2026-06-30
     */
    @GetMapping("/budget-items")
    public Map<Long, BudgetItemLinkStatus> getBatchLinkStatus(
            @RequestParam List<Long> ids,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return savingsLinkStatusService.getBatchLinkStatus(ids, startDate, endDate);
    }
}
