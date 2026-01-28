package com.budget.dto;

import lombok.Data;

@Data
public class UpdateSectionRequest {
    private String name;
    private Boolean isIncome;
}
