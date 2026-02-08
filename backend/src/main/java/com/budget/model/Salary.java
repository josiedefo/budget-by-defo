package com.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "regular_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal regularAmount;

    @Column(name = "federal_tax", nullable = false, precision = 10, scale = 2)
    private BigDecimal federalTax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal medicare;

    @Column(name = "social_security", nullable = false, precision = 10, scale = 2)
    private BigDecimal socialSecurity;

    @Column(name = "four_oh_one_k", precision = 10, scale = 2)
    private BigDecimal fourOhOneK;

    @Column(name = "extra_tax_withholding", precision = 10, scale = 2)
    private BigDecimal extraTaxWithholding;

    @Column(name = "health_savings_account", precision = 10, scale = 2)
    private BigDecimal healthSavingsAccount;

    @Column(name = "medical_insurance", precision = 10, scale = 2)
    private BigDecimal medicalInsurance;

    @Column(name = "flexible_spending_account", precision = 10, scale = 2)
    private BigDecimal flexibleSpendingAccount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
