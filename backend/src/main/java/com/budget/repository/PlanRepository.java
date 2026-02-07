package com.budget.repository;

import com.budget.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByBudgetItemIdAndYearAndMonth(Long budgetItemId, Integer year, Integer month);

    List<Plan> findByYearAndMonthOrderByIdAsc(Integer year, Integer month);

    @Query("SELECT p FROM Plan p WHERE p.budgetItem.id IN :budgetItemIds AND p.year = :year AND p.month = :month")
    List<Plan> findByBudgetItemIdsAndYearAndMonth(
            @Param("budgetItemIds") List<Long> budgetItemIds,
            @Param("year") Integer year,
            @Param("month") Integer month);

    boolean existsByBudgetItemIdAndYearAndMonth(Long budgetItemId, Integer year, Integer month);
}
