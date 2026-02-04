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

    @Query(value = "SELECT t.* FROM transaction t " +
           "LEFT JOIN section s ON s.id = t.section_id " +
           "LEFT JOIN budget_item bi ON bi.id = t.budget_item_id " +
           "WHERE (CAST(:startDate AS DATE) IS NULL OR t.transaction_date >= CAST(:startDate AS DATE)) " +
           "AND (CAST(:endDate AS DATE) IS NULL OR t.transaction_date <= CAST(:endDate AS DATE)) " +
           "AND (CAST(:type AS VARCHAR) IS NULL OR t.type = CAST(:type AS VARCHAR)) " +
           "AND (CAST(:sectionId AS BIGINT) IS NULL OR s.id = CAST(:sectionId AS BIGINT)) " +
           "AND (CAST(:budgetItemId AS BIGINT) IS NULL OR bi.id = CAST(:budgetItemId AS BIGINT)) " +
           "AND (CAST(:sectionName AS VARCHAR) IS NULL OR s.name = CAST(:sectionName AS VARCHAR)) " +
           "AND (CAST(:budgetItemName AS VARCHAR) IS NULL OR bi.name = CAST(:budgetItemName AS VARCHAR)) " +
           "AND (CAST(:merchant AS VARCHAR) IS NULL OR LOWER(t.merchant) LIKE CAST(:merchant AS VARCHAR)) " +
           "ORDER BY t.transaction_date DESC",
           countQuery = "SELECT COUNT(*) FROM transaction t " +
           "LEFT JOIN section s ON s.id = t.section_id " +
           "LEFT JOIN budget_item bi ON bi.id = t.budget_item_id " +
           "WHERE (CAST(:startDate AS DATE) IS NULL OR t.transaction_date >= CAST(:startDate AS DATE)) " +
           "AND (CAST(:endDate AS DATE) IS NULL OR t.transaction_date <= CAST(:endDate AS DATE)) " +
           "AND (CAST(:type AS VARCHAR) IS NULL OR t.type = CAST(:type AS VARCHAR)) " +
           "AND (CAST(:sectionId AS BIGINT) IS NULL OR s.id = CAST(:sectionId AS BIGINT)) " +
           "AND (CAST(:budgetItemId AS BIGINT) IS NULL OR bi.id = CAST(:budgetItemId AS BIGINT)) " +
           "AND (CAST(:sectionName AS VARCHAR) IS NULL OR s.name = CAST(:sectionName AS VARCHAR)) " +
           "AND (CAST(:budgetItemName AS VARCHAR) IS NULL OR bi.name = CAST(:budgetItemName AS VARCHAR)) " +
           "AND (CAST(:merchant AS VARCHAR) IS NULL OR LOWER(t.merchant) LIKE CAST(:merchant AS VARCHAR))",
           nativeQuery = true)
    Page<Transaction> findWithFilters(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("type") String type,
        @Param("sectionId") Long sectionId,
        @Param("budgetItemId") Long budgetItemId,
        @Param("sectionName") String sectionName,
        @Param("budgetItemName") String budgetItemName,
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

    @Query("SELECT t.section.name, t.budgetItem.name, " +
           "SUM(CASE WHEN t.type = com.budget.model.TransactionType.INCOME THEN t.amount ELSE -t.amount END) FROM Transaction t " +
           "WHERE t.budgetItem IS NOT NULL " +
           "AND t.section IS NOT NULL " +
           "AND t.transactionDate >= :startDate " +
           "AND t.transactionDate <= :endDate " +
           "GROUP BY t.section.name, t.budgetItem.name")
    List<Object[]> sumAmountsByBudgetItemNameAndDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
