package com.budget.service;

import com.budget.dto.LinkTransactionToFundRequest;
import com.budget.dto.LogDepositRequest;
import com.budget.dto.ReallocateRequest;
import com.budget.dto.SavingsEventDTO;
import com.budget.dto.UpdateSavingsEventRequest;
import com.budget.model.FundGoalType;
import com.budget.model.SavingsEvent;
import com.budget.model.SavingsEventType;
import com.budget.model.SavingsFund;
import com.budget.model.Transaction;
import com.budget.model.TransactionType;
import com.budget.repository.SavingsAccountRepository;
import com.budget.repository.SavingsEventRepository;
import com.budget.repository.SavingsFundRepository;
import com.budget.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsEventServiceTest {

    @Mock private SavingsFundRepository savingsFundRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private SavingsEventRepository savingsEventRepository;
    @Mock private SavingsFundService savingsFundService;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private SavingsEventService service;

    private SavingsFund fund;

    @BeforeEach
    void setUp() {
        fund = new SavingsFund();
        fund.setId(1L);
        fund.setName("Vacation");
        fund.setGoalType(FundGoalType.NO_GOAL);
        fund.setBalance(new BigDecimal("100.00"));
        fund.setIsSystemFund(false);
        fund.setIsActive(true);

        lenient().when(savingsEventRepository.save(any(SavingsEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(savingsFundRepository.save(any(SavingsFund.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private SavingsEvent depositEvent(BigDecimal amount) {
        SavingsEvent event = new SavingsEvent();
        event.setId(10L);
        event.setFund(fund);
        event.setEventType(SavingsEventType.DEPOSIT_ALLOCATED);
        event.setAmount(amount);
        event.setEventDate(LocalDate.of(2026, 7, 1));
        event.setNote("original note");
        return event;
    }

    private UpdateSavingsEventRequest updateRequest(String amount) {
        UpdateSavingsEventRequest request = new UpdateSavingsEventRequest();
        request.setAmount(new BigDecimal(amount));
        request.setEventDate(LocalDate.of(2026, 7, 15));
        request.setNote("updated note");
        return request;
    }

    // ── Bug: lowering a deposit below what the fund has already spent ──

    @Test
    void updateEvent_loweringDepositBelowSpentAmount_throws() {
        // Fund deposited 500, spent 400 → balance 100. Lowering the deposit to 200
        // would make the balance -200.
        SavingsEvent event = depositEvent(new BigDecimal("500.00"));
        when(savingsEventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.updateEvent(10L, updateRequest("200.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("negative fund balance");

        assertThat(fund.getBalance()).isEqualByComparingTo("100.00");
        verify(savingsFundRepository, never()).save(any());
    }

    @Test
    void updateEvent_raisingDepositBeyondPool_throws() {
        SavingsEvent event = depositEvent(new BigDecimal("50.00"));
        when(savingsEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(savingsFundRepository.sumAllActiveFundBalances()).thenReturn(new BigDecimal("100.00"));
        when(savingsAccountRepository.sumActiveBalances()).thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.updateEvent(10L, updateRequest("100.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceed savings pool");
    }

    @Test
    void updateEvent_validDepositEdit_adjustsBalance() {
        SavingsEvent event = depositEvent(new BigDecimal("50.00"));
        when(savingsEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(savingsFundRepository.sumAllActiveFundBalances()).thenReturn(new BigDecimal("100.00"));
        when(savingsAccountRepository.sumActiveBalances()).thenReturn(new BigDecimal("1000.00"));

        SavingsEventDTO dto = service.updateEvent(10L, updateRequest("75.00"));

        assertThat(fund.getBalance()).isEqualByComparingTo("125.00");
        assertThat(dto.getAmount()).isEqualByComparingTo("75.00");
        assertThat(dto.getNote()).isEqualTo("updated note");
    }

    @Test
    void updateEvent_nullNoteAndDate_preservesExistingValues() {
        SavingsEvent event = depositEvent(new BigDecimal("50.00"));
        when(savingsEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(savingsFundRepository.sumAllActiveFundBalances()).thenReturn(new BigDecimal("100.00"));
        when(savingsAccountRepository.sumActiveBalances()).thenReturn(new BigDecimal("1000.00"));

        UpdateSavingsEventRequest request = new UpdateSavingsEventRequest();
        request.setAmount(new BigDecimal("60.00"));
        // eventDate and note deliberately null

        SavingsEventDTO dto = service.updateEvent(10L, request);

        assertThat(dto.getNote()).isEqualTo("original note");
        assertThat(dto.getEventDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void updateEvent_raisingWithdrawalBeyondBalance_throws() {
        SavingsEvent event = depositEvent(new BigDecimal("30.00"));
        event.setEventType(SavingsEventType.WITHDRAWAL);
        when(savingsEventRepository.findById(10L)).thenReturn(Optional.of(event));

        // balance 100 + old 30 - new 200 = -70
        assertThatThrownBy(() -> service.updateEvent(10L, updateRequest("200.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceed fund balance");
    }

    // ── Bug: lowering a reallocation the destination fund has already spent ──

    @Test
    void updateReallocationEvent_loweringBelowDestinationSpend_throws() {
        SavingsFund destination = new SavingsFund();
        destination.setId(2L);
        destination.setName("Emergency");
        destination.setGoalType(FundGoalType.NO_GOAL);
        destination.setBalance(new BigDecimal("100.00")); // received 500, spent 400

        SavingsEvent outEvent = new SavingsEvent();
        outEvent.setId(20L);
        outEvent.setFund(fund);
        outEvent.setEventType(SavingsEventType.REALLOCATION_OUT);
        outEvent.setAmount(new BigDecimal("500.00"));
        outEvent.setPairedEventId(21L);

        SavingsEvent inEvent = new SavingsEvent();
        inEvent.setId(21L);
        inEvent.setFund(destination);
        inEvent.setEventType(SavingsEventType.REALLOCATION_IN);
        inEvent.setAmount(new BigDecimal("500.00"));
        inEvent.setPairedEventId(20L);

        when(savingsEventRepository.findById(20L)).thenReturn(Optional.of(outEvent));
        when(savingsEventRepository.findById(21L)).thenReturn(Optional.of(inEvent));

        assertThatThrownBy(() -> service.updateEvent(20L, updateRequest("50.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("destination fund");
    }

    @Test
    void updateReallocationEvent_validEdit_movesBalancesOnBothFunds() {
        SavingsFund destination = new SavingsFund();
        destination.setId(2L);
        destination.setName("Emergency");
        destination.setGoalType(FundGoalType.NO_GOAL);
        destination.setBalance(new BigDecimal("500.00"));

        SavingsEvent outEvent = new SavingsEvent();
        outEvent.setId(20L);
        outEvent.setFund(fund);
        outEvent.setEventType(SavingsEventType.REALLOCATION_OUT);
        outEvent.setAmount(new BigDecimal("500.00"));
        outEvent.setPairedEventId(21L);

        SavingsEvent inEvent = new SavingsEvent();
        inEvent.setId(21L);
        inEvent.setFund(destination);
        inEvent.setEventType(SavingsEventType.REALLOCATION_IN);
        inEvent.setAmount(new BigDecimal("500.00"));
        inEvent.setPairedEventId(20L);

        when(savingsEventRepository.findById(20L)).thenReturn(Optional.of(outEvent));
        when(savingsEventRepository.findById(21L)).thenReturn(Optional.of(inEvent));

        service.updateEvent(20L, updateRequest("300.00"));

        // source: 100 + 500 - 300 = 300; destination: 500 - 500 + 300 = 300
        assertThat(fund.getBalance()).isEqualByComparingTo("300.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("300.00");
        assertThat(outEvent.getAmount()).isEqualByComparingTo("300.00");
        assertThat(inEvent.getAmount()).isEqualByComparingTo("300.00");
    }

    // ── Bug: single-transaction fund deposit link skipped pool enforcement ──

    @Test
    void linkTransactionToFund_depositExceedingPool_throws() {
        Transaction tx = transaction(new BigDecimal("200.00"));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        when(savingsEventRepository.findByTransactionRef(5L)).thenReturn(Optional.empty());
        when(savingsFundRepository.findById(1L)).thenReturn(Optional.of(fund));
        when(savingsFundRepository.sumAllActiveFundBalances()).thenReturn(new BigDecimal("900.00"));
        when(savingsAccountRepository.sumActiveBalances()).thenReturn(new BigDecimal("1000.00"));

        LinkTransactionToFundRequest request = new LinkTransactionToFundRequest();
        request.setTransactionId(5L);
        request.setFundId(1L);
        request.setEventType(SavingsEventType.DEPOSIT_ALLOCATED);

        assertThatThrownBy(() -> service.linkTransactionToFund(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceed savings pool");

        assertThat(fund.getBalance()).isEqualByComparingTo("100.00");
        verify(savingsEventRepository, never()).save(any());
    }

    @Test
    void linkTransactionToFund_depositWithinPool_createsEventAndUpdatesBalance() {
        Transaction tx = transaction(new BigDecimal("200.00"));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        when(savingsEventRepository.findByTransactionRef(5L)).thenReturn(Optional.empty());
        when(savingsFundRepository.findById(1L)).thenReturn(Optional.of(fund));
        when(savingsFundRepository.sumAllActiveFundBalances()).thenReturn(new BigDecimal("100.00"));
        when(savingsAccountRepository.sumActiveBalances()).thenReturn(new BigDecimal("1000.00"));

        LinkTransactionToFundRequest request = new LinkTransactionToFundRequest();
        request.setTransactionId(5L);
        request.setFundId(1L);
        request.setEventType(SavingsEventType.DEPOSIT_ALLOCATED);

        SavingsEventDTO dto = service.linkTransactionToFund(request);

        assertThat(fund.getBalance()).isEqualByComparingTo("300.00");
        assertThat(dto.getTransactionRef()).isEqualTo(5L);
        assertThat(dto.getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void linkTransactionToFund_withdrawalBeyondBalance_throws() {
        Transaction tx = transaction(new BigDecimal("200.00"));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        when(savingsEventRepository.findByTransactionRef(5L)).thenReturn(Optional.empty());
        when(savingsFundRepository.findById(1L)).thenReturn(Optional.of(fund));

        LinkTransactionToFundRequest request = new LinkTransactionToFundRequest();
        request.setTransactionId(5L);
        request.setFundId(1L);
        request.setEventType(SavingsEventType.WITHDRAWAL);

        assertThatThrownBy(() -> service.linkTransactionToFund(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void linkTransactionToFund_alreadyLinked_throws() {
        Transaction tx = transaction(new BigDecimal("200.00"));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        when(savingsEventRepository.findByTransactionRef(5L))
                .thenReturn(Optional.of(new SavingsEvent()));

        LinkTransactionToFundRequest request = new LinkTransactionToFundRequest();
        request.setTransactionId(5L);
        request.setFundId(1L);
        request.setEventType(SavingsEventType.DEPOSIT_ALLOCATED);

        assertThatThrownBy(() -> service.linkTransactionToFund(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked");
    }

    // ── Generic flows ──

    @Test
    void logDeposit_exceedingPool_throws() {
        when(savingsFundRepository.findById(1L)).thenReturn(Optional.of(fund));
        when(savingsFundRepository.sumAllActiveFundBalances()).thenReturn(new BigDecimal("950.00"));
        when(savingsAccountRepository.sumActiveBalances()).thenReturn(new BigDecimal("1000.00"));

        LogDepositRequest request = new LogDepositRequest();
        request.setTargetFundId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setEventDate(LocalDate.of(2026, 7, 1));

        assertThatThrownBy(() -> service.logDeposit(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceed savings pool");
    }

    @Test
    void reallocate_movesBalanceBetweenFunds() {
        SavingsFund destination = new SavingsFund();
        destination.setId(2L);
        destination.setName("Emergency");
        destination.setGoalType(FundGoalType.NO_GOAL);
        destination.setBalance(BigDecimal.ZERO);

        when(savingsFundRepository.findById(1L)).thenReturn(Optional.of(fund));
        when(savingsFundRepository.findById(2L)).thenReturn(Optional.of(destination));
        when(savingsEventRepository.save(any(SavingsEvent.class))).thenAnswer(inv -> {
            SavingsEvent e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(e.getEventType() == SavingsEventType.REALLOCATION_OUT ? 30L : 31L);
            }
            return e;
        });

        ReallocateRequest request = new ReallocateRequest();
        request.setSourceFundId(1L);
        request.setDestinationFundId(2L);
        request.setAmount(new BigDecimal("40.00"));

        var events = service.reallocate(request);

        assertThat(fund.getBalance()).isEqualByComparingTo("60.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("40.00");
        assertThat(events).hasSize(2);
    }

    @Test
    void deleteEvent_withdrawal_restoresFundBalance() {
        SavingsEvent event = depositEvent(new BigDecimal("25.00"));
        event.setEventType(SavingsEventType.WITHDRAWAL);
        when(savingsEventRepository.findById(10L)).thenReturn(Optional.of(event));

        service.deleteEvent(10L);

        assertThat(fund.getBalance()).isEqualByComparingTo("125.00");
        verify(savingsEventRepository).delete(event);
    }

    private Transaction transaction(BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setId(5L);
        tx.setType(TransactionType.EXPENSE);
        tx.setTransactionDate(LocalDate.of(2026, 7, 10));
        tx.setMerchant("Test Merchant");
        tx.setAmount(amount);
        return tx;
    }
}
