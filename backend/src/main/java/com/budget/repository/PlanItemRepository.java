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

    List<PlanItem> findBySubscriptionId(Long subscriptionId);

    List<PlanItem> findBySalaryId(Long salaryId);

    /**
     * Finds all plan items linked to a subscription — either via FK (new-style items)
     * or by flag + name match (old-style items created before the FK existed).
     * Also backfill-ready: callers should set subscription on name-matched items.
     */
    @Query("SELECT pi FROM PlanItem pi WHERE pi.subscription.id = :subscriptionId " +
           "OR (pi.fromSubscription = true AND pi.subscription IS NULL AND pi.name = :name)")
    List<PlanItem> findLinkedToSubscription(@Param("subscriptionId") Long subscriptionId,
                                            @Param("name") String name);

    /**
     * Finds all plan items linked to a salary — either via FK (new-style items)
     * or by flag + name match (old-style items created before the FK existed).
     */
    @Query("SELECT pi FROM PlanItem pi WHERE pi.salary.id = :salaryId " +
           "OR (pi.fromSalary = true AND pi.salary IS NULL AND pi.name = :name)")
    List<PlanItem> findLinkedToSalary(@Param("salaryId") Long salaryId,
                                      @Param("name") String name);
}
