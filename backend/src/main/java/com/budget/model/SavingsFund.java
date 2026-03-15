package com.budget.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "savings_fund")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingsFund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private FundGoalType goalType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "target_amount", precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(precision = 12, scale = 2)
    private BigDecimal ceiling;

    @Column(name = "payout_date")
    private LocalDate payoutDate;

    @Column(name = "payout_amount", precision = 12, scale = 2)
    private BigDecimal payoutAmount;

    @Column(name = "is_system_fund", nullable = false)
    private Boolean isSystemFund = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "fund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavingsEvent> events = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
