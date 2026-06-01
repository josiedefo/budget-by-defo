package com.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkLinkResult {
    private int linked;
    private int skipped;   // already linked to this type (account or fund)
    private int total;
    private BigDecimal totalLinkedAmount;
}
