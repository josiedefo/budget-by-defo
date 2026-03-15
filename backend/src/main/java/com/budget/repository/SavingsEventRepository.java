package com.budget.repository;

import com.budget.model.SavingsEvent;
import com.budget.model.SavingsEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SavingsEventRepository extends JpaRepository<SavingsEvent, Long> {

    @Query("SELECT e FROM SavingsEvent e JOIN FETCH e.fund WHERE e.fund.id = :fundId ORDER BY e.eventDate DESC, e.createdAt DESC")
    List<SavingsEvent> findByFundIdOrderByDateDesc(@Param("fundId") Long fundId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM SavingsEvent e " +
           "WHERE e.fund.id = :fundId AND e.eventType = 'WITHDRAWAL' " +
           "AND YEAR(e.eventDate) = :year")
    BigDecimal sumWithdrawalsForFundInYear(@Param("fundId") Long fundId, @Param("year") int year);
}
