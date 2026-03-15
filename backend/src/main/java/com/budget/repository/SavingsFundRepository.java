package com.budget.repository;

import com.budget.model.SavingsFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SavingsFundRepository extends JpaRepository<SavingsFund, Long> {

    List<SavingsFund> findAllByIsActiveTrueOrderByNameAsc();

    Optional<SavingsFund> findByIsSystemFundTrue();

    @Query("SELECT COALESCE(SUM(f.balance), 0) FROM SavingsFund f WHERE f.isActive = true")
    BigDecimal sumAllActiveFundBalances();

    @Query("SELECT f FROM SavingsFund f WHERE f.isActive = true " +
           "AND f.goalType IN ('TARGET_WITH_DEADLINE', 'SPEND_DOWN') " +
           "AND (f.deadline IS NOT NULL OR f.payoutDate IS NOT NULL) " +
           "ORDER BY CASE WHEN f.deadline IS NOT NULL THEN f.deadline ELSE f.payoutDate END ASC")
    List<SavingsFund> findUpcomingDeadlineFunds();
}
