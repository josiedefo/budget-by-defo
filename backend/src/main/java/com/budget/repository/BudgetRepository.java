package com.budget.repository;

import com.budget.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByYearAndMonth(Integer year, Integer month);

    List<Budget> findByYearOrderByMonthAsc(Integer year);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.sections s LEFT JOIN FETCH s.items WHERE b.year = :year AND b.month = :month")
    Optional<Budget> findByYearAndMonthWithSectionsAndItems(Integer year, Integer month);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.sections s LEFT JOIN FETCH s.items WHERE b.id = :id")
    Optional<Budget> findByIdWithSectionsAndItems(Long id);

    boolean existsByYearAndMonth(Integer year, Integer month);
}
