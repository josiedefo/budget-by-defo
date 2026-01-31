package com.budget.service;

import com.budget.dto.CreateTransactionRequest;
import com.budget.dto.TransactionDTO;
import com.budget.dto.UpdateTransactionRequest;
import com.budget.model.BudgetItem;
import com.budget.model.Section;
import com.budget.model.Transaction;
import com.budget.model.TransactionType;
import com.budget.repository.BudgetItemRepository;
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

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SectionRepository sectionRepository;
    private final BudgetItemRepository budgetItemRepository;

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactions(
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long sectionId,
            Long budgetItemId,
            String merchant,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));

        // Prepare merchant pattern for LIKE query
        String merchantPattern = (merchant != null && !merchant.isBlank())
            ? "%" + merchant.toLowerCase() + "%"
            : null;

        Page<Transaction> transactions = transactionRepository.findWithFilters(
            startDate,
            endDate,
            type,
            sectionId,
            budgetItemId,
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
}
