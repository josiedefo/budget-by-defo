package com.budget.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdatePlanRequest {
    @NotNull
    private List<PlanItemInput> items;

    @Data
    public static class PlanItemInput {
        private String name;
        private BigDecimal amount;
        private Boolean fromSubscription;
    }
}
