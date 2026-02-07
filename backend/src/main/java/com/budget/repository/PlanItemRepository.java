package com.budget.repository;

import com.budget.model.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

    List<PlanItem> findByPlanIdOrderByDisplayOrderAsc(Long planId);

    @Modifying
    @Query("DELETE FROM PlanItem pi WHERE pi.plan.id = :planId")
    void deleteByPlanId(@Param("planId") Long planId);
}
