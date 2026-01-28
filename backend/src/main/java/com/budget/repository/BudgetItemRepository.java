package com.budget.repository;

import com.budget.model.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {

    List<BudgetItem> findBySectionIdOrderByDisplayOrderAsc(Long sectionId);

    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) FROM BudgetItem i WHERE i.section.id = :sectionId")
    Integer findMaxDisplayOrderBySectionId(Long sectionId);
}
