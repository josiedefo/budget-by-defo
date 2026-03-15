package com.budget.repository;

import com.budget.model.SavingsAccountEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SavingsAccountEventRepository extends JpaRepository<SavingsAccountEvent, Long> {

    @Query("SELECT e FROM SavingsAccountEvent e JOIN FETCH e.account " +
           "WHERE e.account.id = :accountId ORDER BY e.eventDate DESC, e.createdAt DESC")
    List<SavingsAccountEvent> findByAccountIdOrderByDateDesc(@Param("accountId") Long accountId);

    @Query("SELECT e.account.id, MAX(e.eventDate) FROM SavingsAccountEvent e " +
           "WHERE e.account.id IN :accountIds GROUP BY e.account.id")
    List<Object[]> findLatestEventDatesForAccounts(@Param("accountIds") List<Long> accountIds);

    @Query("SELECT MAX(e.eventDate) FROM SavingsAccountEvent e WHERE e.account.id = :accountId")
    LocalDate findLatestEventDateByAccountId(@Param("accountId") Long accountId);
}
