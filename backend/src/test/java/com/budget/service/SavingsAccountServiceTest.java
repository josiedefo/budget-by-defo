package com.budget.service;

import com.budget.dto.AccountDepositRequest;
import com.budget.dto.AccountWithdrawalRequest;
import com.budget.dto.BulkLinkBudgetItemRequest;
import com.budget.dto.BulkLinkResult;
import com.budget.dto.LinkTransactionToAccountRequest;
import com.budget.dto.SavingsAccountEventDTO;
import com.budget.dto.UpdateSavingsAccountEventRequest;
import com.budget.model.SavingsAccount;
import com.budget.model.SavingsAccountEvent;
import com.budget.model.SavingsAccountEventType;
import com.budget.model.Transaction;
import com.budget.model.TransactionType;
import com.budget.repository.SavingsAccountEventRepository;
import com.budget.repository.SavingsAccountRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsAccountServiceTest {

    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private SavingsAccountEventRepository savingsAccountEventRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private SavingsAccountService service;

    private SavingsAccount account;

    @BeforeEach
    void setUp() {
        account = new SavingsAccount();
        account.setId(1L);
        account.setName("Ally");
        account.setBalance(new BigDecimal("500.00"));
        account.setIsActive(true);

        lenient().when(savingsAccountRepository.save(any(SavingsAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(savingsAccountEventRepository.save(any(SavingsAccountEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private SavingsAccountEvent event(SavingsAccountEventType type, String amount) {
        SavingsAccountEvent event = new SavingsAccountEvent();
        event.setId(10L);
        event.setAccount(account);
        event.setEventType(type);
        event.setAmount(new BigDecimal(amount));
        event.setEventDate(LocalDate.of(2026, 7, 1));
        event.setNote("original note");
        return event;
    }

    // ── Bug: null note/eventDate on edit used to wipe the stored values ──

    @Test
    void updateAccountEvent_nullNoteAndDate_preservesExistingValues() {
        SavingsAccountEvent existing = event(SavingsAccountEventType.DEPOSIT, "100.00");
        when(savingsAccountEventRepository.findById(10L)).thenReturn(Optional.of(existing));

        UpdateSavingsAccountEventRequest request = new UpdateSavingsAccountEventRequest();
        request.setAmount(new BigDecimal("120.00"));
        // note and eventDate deliberately null

        SavingsAccountEventDTO dto = service.updateAccountEvent(10L, request);

        assertThat(dto.getNote()).isEqualTo("original note");
        assertThat(dto.getEventDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(account.getBalance()).isEqualByComparingTo("520.00");
    }

    @Test
    void updateAccountEvent_loweringDepositBelowBalance_throws() {
        account.setBalance(new BigDecimal("20.00"));
        SavingsAccountEvent existing = event(SavingsAccountEventType.DEPOSIT, "100.00");
        when(savingsAccountEventRepository.findById(10L)).thenReturn(Optional.of(existing));

        UpdateSavingsAccountEventRequest request = new UpdateSavingsAccountEventRequest();
        request.setAmount(new BigDecimal("5.00"));
        request.setEventDate(LocalDate.of(2026, 7, 2));

        assertThatThrownBy(() -> service.updateAccountEvent(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("negative account balance");
    }

    // ── Generic flows ──

    @Test
    void logDeposit_addsToBalanceAndRecordsEvent() {
        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountDepositRequest request = new AccountDepositRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setEventDate(LocalDate.of(2026, 7, 3));

        SavingsAccountEventDTO dto = service.logDeposit(1L, request);

        assertThat(account.getBalance()).isEqualByComparingTo("550.00");
        assertThat(dto.getBalanceAfter()).isEqualByComparingTo("550.00");
        assertThat(dto.getEventType()).isEqualTo(SavingsAccountEventType.DEPOSIT);
    }

    @Test
    void logWithdrawal_insufficientBalance_throws() {
        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountWithdrawalRequest request = new AccountWithdrawalRequest();
        request.setAmount(new BigDecimal("600.00"));
        request.setEventDate(LocalDate.of(2026, 7, 3));

        assertThatThrownBy(() -> service.logWithdrawal(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient account balance");
    }

    @Test
    void linkTransactionToAccount_alreadyLinked_throws() {
        Transaction tx = transaction(5L, "100.00");
        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        when(savingsAccountEventRepository.findByTransactionId(5L))
                .thenReturn(Optional.of(new SavingsAccountEvent()));

        LinkTransactionToAccountRequest request = new LinkTransactionToAccountRequest();
        request.setTransactionId(5L);
        request.setEventType(SavingsAccountEventType.DEPOSIT);

        assertThatThrownBy(() -> service.linkTransactionToAccount(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    void bulkLinkBudgetItem_skipsAlreadyLinkedTransactions() {
        Transaction linked = transaction(5L, "100.00");
        Transaction unlinked = transaction(6L, "40.00");

        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.findByBudgetItemIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                any(), any(), any())).thenReturn(List.of(linked, unlinked));

        SavingsAccountEvent existingLink = new SavingsAccountEvent();
        existingLink.setTransaction(linked);
        when(savingsAccountEventRepository.findByTransactionIdIn(anyList()))
                .thenReturn(List.of(existingLink));

        BulkLinkBudgetItemRequest request = new BulkLinkBudgetItemRequest();
        request.setBudgetItemId(99L);
        request.setEventType(SavingsAccountEventType.DEPOSIT);
        request.setStartDate(LocalDate.of(2026, 7, 1));
        request.setEndDate(LocalDate.of(2026, 7, 31));

        BulkLinkResult result = service.bulkLinkBudgetItem(1L, request);

        assertThat(result.getLinked()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getTotalLinkedAmount()).isEqualByComparingTo("40.00");
        assertThat(account.getBalance()).isEqualByComparingTo("540.00");
        verify(savingsAccountEventRepository, times(1)).save(any(SavingsAccountEvent.class));
    }

    private Transaction transaction(Long id, String amount) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setType(TransactionType.EXPENSE);
        tx.setTransactionDate(LocalDate.of(2026, 7, 10));
        tx.setMerchant("Merchant " + id);
        tx.setAmount(new BigDecimal(amount));
        return tx;
    }
}
