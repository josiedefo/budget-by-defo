package com.budget.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingsSummaryDTO {
    private BigDecimal totalPool;
    private BigDecimal totalAllocated;
    private BigDecimal totalUnallocated;
    private BigDecimal totalRemainingToSave;
    private List<SavingsFundDTO> upcomingDeadlines;
}
