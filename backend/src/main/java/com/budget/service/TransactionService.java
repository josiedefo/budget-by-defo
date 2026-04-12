package com.budget.service;

import com.budget.dto.CreateTransactionRequest;
import com.budget.dto.CsvImportRequest;
import com.budget.dto.TransactionDTO;
import com.budget.dto.UpdateTransactionRequest;
import com.budget.model.Budget;
import com.budget.model.BudgetItem;
import com.budget.model.Section;
import com.budget.model.Transaction;
import com.budget.model.TransactionType;
import com.budget.repository.BudgetItemRepository;
import com.budget.repository.BudgetRepository;
import com.budget.repository.SectionRepository;
import com.budget.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SectionRepository sectionRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactions(
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long sectionId,
            Long budgetItemId,
            String sectionName,
            String budgetItemName,
            String merchant,
            int page,
            int size) {
        // Use unsorted pageable since native query already has ORDER BY clause
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());

        // Prepare merchant pattern for LIKE query
        String merchantPattern = (merchant != null && !merchant.isBlank())
            ? "%" + merchant.toLowerCase() + "%"
            : null;

        // Convert enum to string for native query
        String typeStr = type != null ? type.name() : null;

        Page<Transaction> transactions = transactionRepository.findWithFilters(
            startDate,
            endDate,
            typeStr,
            sectionId,
            budgetItemId,
            sectionName,
            budgetItemName,
            merchantPattern,
            pageable
        );

        // Convert within the transaction to ensure lazy loading works
        List<TransactionDTO> dtos = transactions.getContent().stream()
            .map(TransactionDTO::fromEntity)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, transactions.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TransactionDTO getTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));
        return TransactionDTO.fromEntity(transaction);
    }

    @Transactional
    public TransactionDTO createTransaction(CreateTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setType(request.getType());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setMerchant(request.getMerchant());
        transaction.setAmount(request.getAmount());
        transaction.setNote(request.getNote());

        if (request.getSectionId() != null) {
            Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new EntityNotFoundException("Section not found with id: " + request.getSectionId()));
            transaction.setSection(section);
        }

        if (request.getBudgetItemId() != null) {
            BudgetItem budgetItem = budgetItemRepository.findById(request.getBudgetItemId())
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found with id: " + request.getBudgetItemId()));
            transaction.setBudgetItem(budgetItem);
        }

        transaction = transactionRepository.save(transaction);
        return TransactionDTO.fromEntity(transaction);
    }

    @Transactional
    public TransactionDTO updateTransaction(Long id, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));

        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }
        if (request.getMerchant() != null) {
            transaction.setMerchant(request.getMerchant());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getNote() != null) {
            transaction.setNote(request.getNote());
        }

        if (request.getSectionId() != null) {
            Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new EntityNotFoundException("Section not found"));
            transaction.setSection(section);
        }

        if (request.getBudgetItemId() != null) {
            BudgetItem item = budgetItemRepository.findById(request.getBudgetItemId())
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
            transaction.setBudgetItem(item);
        }

        transaction = transactionRepository.save(transaction);
        return TransactionDTO.fromEntity(transaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new EntityNotFoundException("Transaction not found with id: " + id);
        }
        transactionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalByType(TransactionType type, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = transactionRepository.sumAmountByTypeAndDateRange(type, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public List<TransactionDTO> importTransactions(CsvImportRequest request) {
        List<TransactionDTO> imported = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
            request.getDateFormat() != null ? request.getDateFormat() : "yyyy-MM-dd"
        );

        List<List<String>> rows = request.getRows();
        int startIndex = request.isSkipFirstRow() ? 1 : 0;

        for (int i = startIndex; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                Transaction transaction = new Transaction();

                // Parse date
                Integer dateCol = request.getColumnMapping().get("date");
                if (dateCol != null && dateCol < row.size()) {
                    String dateStr = row.get(dateCol).trim();
                    transaction.setTransactionDate(LocalDate.parse(dateStr, dateFormatter));
                } else {
                    continue; // Skip rows without date
                }

                // Parse merchant
                Integer merchantCol = request.getColumnMapping().get("merchant");
                if (merchantCol != null && merchantCol < row.size()) {
                    transaction.setMerchant(row.get(merchantCol).trim());
                } else {
                    transaction.setMerchant("Unknown");
                }

                // Parse amount; positive = INCOME, negative = EXPENSE
                Integer amountCol = request.getColumnMapping().get("amount");
                if (amountCol != null && amountCol < row.size()) {
                    String amountStr = row.get(amountCol).trim()
                        .replace("$", "")
                        .replace(",", "")
                        .replace("(", "-")
                        .replace(")", "");
                    BigDecimal rawAmount = new BigDecimal(amountStr);
                    transaction.setAmount(rawAmount.abs());
                    transaction.setType(rawAmount.signum() >= 0 ? TransactionType.INCOME : TransactionType.EXPENSE);
                } else {
                    continue; // Skip rows without amount
                }

                // Parse note (optional)
                Integer noteCol = request.getColumnMapping().get("note");
                if (noteCol != null && noteCol < row.size()) {
                    transaction.setNote(row.get(noteCol).trim());
                }

                // Look up (but do NOT create) the budget for this transaction's month
                LocalDate txDate = transaction.getTransactionDate();
                Budget budget = budgetRepository.findByYearAndMonth(txDate.getYear(), txDate.getMonthValue())
                    .orElse(null);

                // Parse category/section (optional) - match within this budget month only if it exists
                Integer categoryCol = request.getColumnMapping().get("category");
                Section matchedSection = null;
                if (budget != null && categoryCol != null && categoryCol < row.size()) {
                    String categoryName = row.get(categoryCol).trim();
                    if (!categoryName.isEmpty()) {
                        final String catName = categoryName;
                        matchedSection = sectionRepository.findByBudgetIdOrderByDisplayOrderAsc(budget.getId())
                            .stream()
                            .filter(s -> s.getName().equalsIgnoreCase(catName))
                            .findFirst()
                            .orElse(null);
                        if (matchedSection != null) {
                            transaction.setSection(matchedSection);
                        }
                    }
                }

                // Parse budget item (optional) - match or auto-create within the matched section
                Integer budgetItemCol = request.getColumnMapping().get("budgetItem");
                if (budgetItemCol != null && budgetItemCol < row.size() && matchedSection != null) {
                    String budgetItemName = row.get(budgetItemCol).trim();
                    if (!budgetItemName.isEmpty()) {
                        final Section section = matchedSection;
                        final String itemName = budgetItemName;
                        BudgetItem matched = budgetItemRepository
                            .findBySectionIdOrderByDisplayOrderAsc(section.getId())
                            .stream()
                            .filter(bi -> bi.getName().equalsIgnoreCase(itemName))
                            .findFirst()
                            .orElse(null);
                        if (matched == null) {
                            // Auto-create the budget item in this section
                            int nextOrder = budgetItemRepository.findMaxDisplayOrderBySectionId(section.getId()) + 1;
                            BudgetItem newItem = new BudgetItem();
                            newItem.setName(itemName);
                            newItem.setSection(section);
                            newItem.setPlannedAmount(BigDecimal.ZERO);
                            newItem.setActualAmount(BigDecimal.ZERO);
                            newItem.setDisplayOrder(nextOrder);
                            matched = budgetItemRepository.save(newItem);
                        }
                        transaction.setBudgetItem(matched);
                    }
                }

                Transaction saved = transactionRepository.save(transaction);
                imported.add(TransactionDTO.fromEntity(saved));
            } catch (DateTimeParseException | NumberFormatException e) {
                // Skip invalid rows
                continue;
            }
        }

        return imported;
    }
}
