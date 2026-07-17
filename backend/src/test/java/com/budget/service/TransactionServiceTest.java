package com.budget.service;

import com.budget.dto.CsvImportRequest;
import com.budget.dto.TransactionDTO;
import com.budget.dto.UpdateTransactionRequest;
import com.budget.model.SavingsAccount;
import com.budget.model.SavingsAccountEvent;
import com.budget.model.SavingsAccountEventType;
import com.budget.model.SavingsEvent;
import com.budget.model.SavingsEventType;
import com.budget.model.SavingsFund;
import com.budget.model.Transaction;
import com.budget.model.TransactionType;
import com.budget.repository.BudgetItemRepository;
import com.budget.repository.BudgetRepository;
import com.budget.repository.SavingsAccountEventRepository;
import com.budget.repository.SavingsAccountRepository;
import com.budget.repository.SavingsEventRepository;
import com.budget.repository.SavingsFundRepository;
import com.budget.repository.SectionRepository;
import com.budget.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private BudgetItemRepository budgetItemRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private SavingsAccountEventRepository savingsAccountEventRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private SavingsEventRepository savingsEventRepository;
    @Mock private SavingsFundRepository savingsFundRepository;

    @InjectMocks private TransactionService service;

    private Transaction transaction;
    private SavingsAccount account;
    private SavingsFund fund;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.setId(5L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setTransactionDate(LocalDate.of(2026, 7, 10));
        transaction.setMerchant("Merchant");
        transaction.setAmount(new BigDecimal("100.00"));

        account = new SavingsAccount();
        account.setId(1L);
        account.setName("Ally");
        account.setBalance(new BigDecimal("500.00"));

        fund = new SavingsFund();
        fund.setId(2L);
        fund.setName("Vacation");
        fund.setBalance(new BigDecimal("300.00"));

        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(savingsAccountRepository.save(any(SavingsAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(savingsFundRepository.save(any(SavingsFund.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(savingsAccountEventRepository.save(any(SavingsAccountEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(savingsEventRepository.save(any(SavingsEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private SavingsAccountEvent accountEvent(SavingsAccountEventType type) {
        SavingsAccountEvent event = new SavingsAccountEvent();
        event.setId(10L);
        event.setAccount(account);
        event.setEventType(type);
        event.setAmount(transaction.getAmount());
        event.setEventDate(transaction.getTransactionDate());
        event.setTransaction(transaction);
        return event;
    }

    private SavingsEvent fundEvent(SavingsEventType type) {
        SavingsEvent event = new SavingsEvent();
        event.setId(20L);
        event.setFund(fund);
        event.setEventType(type);
        event.setAmount(transaction.getAmount());
        event.setEventDate(transaction.getTransactionDate());
        event.setTransactionRef(transaction.getId());
        return event;
    }

    // ── Bug: editing a linked transaction used to leave the savings side stale ──

    @Test
    void updateTransaction_amountChange_syncsLinkedAccountDeposit() {
        SavingsAccountEvent event = accountEvent(SavingsAccountEventType.DEPOSIT);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(savingsAccountEventRepository.findByTransactionId(5L)).thenReturn(Optional.of(event));
        when(savingsEventRepository.findByTransactionRef(5L)).thenReturn(Optional.empty());

        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setAmount(new BigDecimal("150.00"));

        service.updateTransaction(5L, request);

        assertThat(account.getBalance()).isEqualByComparingTo("550.00");
        assertThat(event.getAmount()).isEqualByComparingTo("150.00");
        assertThat(event.getBalanceAfter()).isEqualByComparingTo("550.00");
    }

    @Test
    void updateTransaction_amountChange_syncsLinkedFundWithdrawal() {
        SavingsEvent event = fundEvent(SavingsEventType.WITHDRAWAL);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(savingsAccountEventRepository.findByTransactionId(5L)).thenReturn(Optional.empty());
        when(savingsEventRepository.findByTransactionRef(5L)).thenReturn(Optional.of(event));

        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setAmount(new BigDecimal("150.00"));

        service.updateTransaction(5L, request);

        // Withdrawal grew by 50 → fund loses 50 more
        assertThat(fund.getBalance()).isEqualByComparingTo("250.00");
        assertThat(event.getAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void updateTransaction_dateChange_syncsLinkedEventDates() {
        SavingsAccountEvent event = accountEvent(SavingsAccountEventType.DEPOSIT);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(savingsAccountEventRepository.findByTransactionId(5L)).thenReturn(Optional.of(event));
        when(savingsEventRepository.findByTransactionRef(5L)).thenReturn(Optional.empty());

        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setTransactionDate(LocalDate.of(2026, 7, 20));

        service.updateTransaction(5L, request);

        assertThat(event.getEventDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        // Amount unchanged → balance untouched
        assertThat(account.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void updateTransaction_loweringLinkedDepositBelowAccountBalance_throws() {
        account.setBalance(new BigDecimal("20.00")); // deposited 100, mostly withdrawn since
        SavingsAccountEvent event = accountEvent(SavingsAccountEventType.DEPOSIT);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(savingsAccountEventRepository.findByTransactionId(5L)).thenReturn(Optional.of(event));

        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setAmount(new BigDecimal("5.00"));

        assertThatThrownBy(() -> service.updateTransaction(5L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("balance negative");
    }

    @Test
    void updateTransaction_noAmountOrDateChange_doesNotTouchSavings() {
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));

        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setMerchant("New Merchant");

        service.updateTransaction(5L, request);

        verify(savingsAccountEventRepository, never()).findByTransactionId(anyLong());
        verify(savingsEventRepository, never()).findByTransactionRef(anyLong());
    }

    // ── Delete cascade ──

    @Test
    void deleteTransaction_reversesAccountAndFundBalances() {
        SavingsAccountEvent aEvent = accountEvent(SavingsAccountEventType.DEPOSIT);
        SavingsEvent fEvent = fundEvent(SavingsEventType.WITHDRAWAL);
        when(transactionRepository.existsById(5L)).thenReturn(true);
        when(savingsAccountEventRepository.findByTransactionId(5L)).thenReturn(Optional.of(aEvent));
        when(savingsEventRepository.findByTransactionRef(5L)).thenReturn(Optional.of(fEvent));

        service.deleteTransaction(5L);

        assertThat(account.getBalance()).isEqualByComparingTo("400.00"); // deposit reversed
        assertThat(fund.getBalance()).isEqualByComparingTo("400.00");    // withdrawal reversed
        verify(savingsAccountEventRepository).delete(aEvent);
        verify(savingsEventRepository).delete(fEvent);
        verify(transactionRepository).deleteById(5L);
    }

    @Test
    void deleteTransaction_depositReversalWouldGoNegative_throws() {
        account.setBalance(new BigDecimal("20.00"));
        SavingsAccountEvent aEvent = accountEvent(SavingsAccountEventType.DEPOSIT);
        when(transactionRepository.existsById(5L)).thenReturn(true);
        when(savingsAccountEventRepository.findByTransactionId(5L)).thenReturn(Optional.of(aEvent));

        assertThatThrownBy(() -> service.deleteTransaction(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("balance negative");

        verify(transactionRepository, never()).deleteById(anyLong());
    }

    // ── CSV import ──

    @Test
    void importTransactions_parsesRowsAndSkipsInvalidOnes() {
        CsvImportRequest request = new CsvImportRequest();
        request.setColumnMapping(Map.of("date", 0, "merchant", 1, "amount", 2));
        request.setSkipFirstRow(true);
        request.setRows(List.of(
                List.of("Date", "Merchant", "Amount"),          // header, skipped
                List.of("2026-07-01", "Grocer", "-45.50"),      // expense
                List.of("2026-07-02", "Employer", "1000.00"),   // income
                List.of("not-a-date", "Broken", "10.00"),       // invalid date, skipped
                List.of("2026-07-03", "Zero Corp", "0.00")      // zero amount, skipped
        ));

        when(budgetRepository.findByYearAndMonth(2026, 7)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        List<TransactionDTO> imported = service.importTransactions(request);

        assertThat(imported).hasSize(2);
        assertThat(imported.get(0).getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(imported.get(0).getAmount()).isEqualByComparingTo("45.50");
        assertThat(imported.get(1).getType()).isEqualTo(TransactionType.INCOME);
        assertThat(imported.get(1).getAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void importTransactions_parsesParenthesizedNegativeAmounts() {
        CsvImportRequest request = new CsvImportRequest();
        request.setColumnMapping(Map.of("date", 0, "merchant", 1, "amount", 2));
        request.setSkipFirstRow(false);
        request.setRows(List.of(List.of("2026-07-01", "Grocer", "($1,234.56)")));

        when(budgetRepository.findByYearAndMonth(2026, 7)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        List<TransactionDTO> imported = service.importTransactions(request);

        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(imported.get(0).getAmount()).isEqualByComparingTo("1234.56");
    }

    @Test
    void bulkDeleteTransactions_deletesAllAndTheirLinks() {
        when(savingsAccountEventRepository.findByTransactionId(anyLong())).thenReturn(Optional.empty());
        when(savingsEventRepository.findByTransactionRef(anyLong())).thenReturn(Optional.empty());

        service.bulkDeleteTransactions(List.of(5L, 6L));

        verify(savingsAccountEventRepository).findByTransactionId(5L);
        verify(savingsAccountEventRepository).findByTransactionId(6L);
        verify(transactionRepository).deleteAllByIdInBatch(List.of(5L, 6L));
    }
}
