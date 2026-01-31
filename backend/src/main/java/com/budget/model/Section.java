package com.budget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "section")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    @JsonIgnore
    private Budget budget;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_income", nullable = false)
    private Boolean isIncome = false;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 25)
    private Set<BudgetItem> items = new LinkedHashSet<>();

    public Section(String name, Integer displayOrder, Boolean isIncome) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.isIncome = isIncome;
    }

    // Section.java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Section section = (Section) o;
    return Objects.equals(id, section.id) &&
           Objects.equals(name, section.name) &&
           Objects.equals(displayOrder, section.displayOrder);
    // DON'T include budget or items!
}

@Override
public int hashCode() {
    return Objects.hash(id, name, displayOrder);
    // DON'T include budget or items!
}
}
