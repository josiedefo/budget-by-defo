package com.budget.repository;

import com.budget.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    List<SavingsAccount> findAllByIsActiveTrueOrderByNameAsc();

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM SavingsAccount a WHERE a.isActive = true")
    BigDecimal sumActiveBalances();
}
