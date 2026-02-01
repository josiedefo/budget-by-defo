package com.budget.repository;

import com.budget.model.Transaction;
import com.budget.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
        LocalDate startDate, LocalDate endDate);

    List<Transaction> findByTypeOrderByTransactionDateDesc(TransactionType type);

    List<Transaction> findBySectionIdOrderByTransactionDateDesc(Long sectionId);

    List<Transaction> findByBudgetItemIdOrderByTransactionDateDesc(Long budgetItemId);

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN t.section s " +
           "LEFT JOIN t.budgetItem bi " +
           "WHERE (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:sectionId IS NULL OR s.id = :sectionId) " +
           "AND (:budgetItemId IS NULL OR bi.id = :budgetItemId) " +
           "AND (:merchant IS NULL OR LOWER(CAST(t.merchant AS string)) LIKE :merchant)")
    Page<Transaction> findWithFilters(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("type") TransactionType type,
        @Param("sectionId") Long sectionId,
        @Param("budgetItemId") Long budgetItemId,
        @Param("merchant") String merchant,
        Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type AND " +
           "t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTypeAndDateRange(
        @Param("type") TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT t.budgetItem.id, SUM(t.amount) FROM Transaction t " +
           "WHERE t.budgetItem IS NOT NULL " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate " +
           "GROUP BY t.budgetItem.id")
    List<Object[]> sumAmountsByBudgetItemAndDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT t.section.name, t.budgetItem.name, SUM(t.amount) FROM Transaction t " +
           "WHERE t.budgetItem IS NOT NULL " +
           "AND t.section IS NOT NULL " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate " +
           "GROUP BY t.section.name, t.budgetItem.name")
    List<Object[]> sumAmountsByBudgetItemNameAndDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
