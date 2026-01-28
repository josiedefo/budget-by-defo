package com.budget.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateItemRequest {
    private String name;
    private BigDecimal plannedAmount;
    private BigDecimal actualAmount;
}
