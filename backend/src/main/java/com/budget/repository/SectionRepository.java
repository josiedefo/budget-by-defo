package com.budget.repository;

import com.budget.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByBudgetIdOrderByDisplayOrderAsc(Long budgetId);

    @Query("SELECT COALESCE(MAX(s.displayOrder), 0) FROM Section s WHERE s.budget.id = :budgetId")
    Integer findMaxDisplayOrderByBudgetId(Long budgetId);

    @Query("SELECT s FROM Section s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<Section> findByIdWithItems(Long id);
}
