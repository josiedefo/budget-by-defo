package com.budget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @JsonIgnore
    private Section section;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "planned_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal plannedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public BudgetItem(String name, BigDecimal plannedAmount, Integer displayOrder) {
        this.name = name;
        this.plannedAmount = plannedAmount;
        this.actualAmount = BigDecimal.ZERO;
        this.displayOrder = displayOrder;
    }
}
