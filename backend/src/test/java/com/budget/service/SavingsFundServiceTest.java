package com.budget.service;

import com.budget.dto.SavingsFundDTO;
import com.budget.model.FundGoalType;
import com.budget.model.FundStatus;
import com.budget.model.SavingsEvent;
import com.budget.model.SavingsEventType;
import com.budget.model.SavingsFund;
import com.budget.repository.SavingsAccountRepository;
import com.budget.repository.SavingsEventRepository;
import com.budget.repository.SavingsFundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsFundServiceTest {

    @Mock private SavingsFundRepository savingsFundRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private SavingsEventRepository savingsEventRepository;

    @InjectMocks private SavingsFundService service;

    private SavingsFund fund;

    @BeforeEach
    void setUp() {
        fund = new SavingsFund();
        fund.setId(1L);
        fund.setName("Car Fund");
        fund.setGoalType(FundGoalType.NO_GOAL);
        fund.setBalance(new BigDecimal("150.00"));
        fund.setIsSystemFund(false);
        fund.setIsActive(true);
        when(savingsFundRepository.findById(1L)).thenReturn(Optional.of(fund));
        lenient().when(savingsEventRepository.sumAmountForFundByTypes(eq(1L), anyList()))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void closedTargetFund_keepsProgressFromLifetimeSavings_notBalance() {
        fund.setName("Kids School Fund");
        fund.setGoalType(FundGoalType.TARGET_WITH_DEADLINE);
        fund.setTargetAmount(new BigDecimal("19880.00"));
        fund.setDeadline(LocalDate.of(2026, 8, 1));
        fund.setBalance(new BigDecimal("19880.00"));
        when(savingsEventRepository.sumAmountForFundByTypes(1L,
                List.of(SavingsEventType.DEPOSIT_ALLOCATED, SavingsEventType.REALLOCATION_IN)))
                .thenReturn(new BigDecimal("19880.00"));
        when(savingsEventRepository.sumAmountForFundByTypes(1L,
                List.of(SavingsEventType.WITHDRAWAL, SavingsEventType.PAYOUT)))
                .thenReturn(new BigDecimal("16805.00"));
        when(savingsEventRepository.sumAmountForFundByTypes(1L, List.of(SavingsEventType.CLOSE_RELEASE)))
                .thenReturn(new BigDecimal("3075.00"));
        fund.setClosedDate(LocalDate.of(2026, 8, 5));
        fund.setBalance(BigDecimal.ZERO);

        SavingsFundDTO dto = service.getFund(1L);

        assertThat(dto.getProgressPercent()).isEqualTo(100);
        assertThat(dto.getStatus()).isEqualTo(FundStatus.COMPLETE);
        assertThat(dto.getRemaining()).isEqualByComparingTo("0");
        assertThat(dto.getTotalSaved()).isEqualByComparingTo("19880.00");
        assertThat(dto.getTotalUsed()).isEqualByComparingTo("16805.00");
        assertThat(dto.getReleasedAmount()).isEqualByComparingTo("3075.00");
    }

    @Test
    void closedTargetFund_savedLessThanTarget_isClosedNotComplete() {
        fund.setGoalType(FundGoalType.TARGET);
        fund.setTargetAmount(new BigDecimal("1000.00"));
        fund.setClosedDate(LocalDate.of(2026, 8, 5));
        when(savingsEventRepository.sumAmountForFundByTypes(1L,
                List.of(SavingsEventType.DEPOSIT_ALLOCATED, SavingsEventType.REALLOCATION_IN)))
                .thenReturn(new BigDecimal("400.00"));

        SavingsFundDTO dto = service.getFund(1L);

        assertThat(dto.getStatus()).isEqualTo(FundStatus.CLOSED);
        assertThat(dto.getProgressPercent()).isEqualTo(40);
        assertThat(dto.getRemaining()).isEqualByComparingTo("600.00");
    }

    @Test
    void closeFund_releasesBalanceRecordsEventAndMarksClosed() {
        LocalDate closeDate = LocalDate.of(2026, 9, 30);

        SavingsFundDTO dto = service.closeFund(1L, closeDate);

        assertThat(fund.getBalance()).isEqualByComparingTo("0.00");
        assertThat(fund.getClosedDate()).isEqualTo(closeDate);
        assertThat(fund.getIsActive()).isTrue();
        assertThat(dto.getIsClosed()).isTrue();
        assertThat(dto.getClosedDate()).isEqualTo(closeDate);

        ArgumentCaptor<SavingsEvent> captor = ArgumentCaptor.forClass(SavingsEvent.class);
        verify(savingsEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(SavingsEventType.CLOSE_RELEASE);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("150.00");
        assertThat(captor.getValue().getEventDate()).isEqualTo(closeDate);
    }

    @Test
    void closeFund_withZeroBalance_recordsNoEvent() {
        fund.setBalance(BigDecimal.ZERO);

        service.closeFund(1L, LocalDate.of(2026, 9, 30));

        assertThat(fund.isClosed()).isTrue();
        verify(savingsEventRepository, never()).save(any());
    }

    @Test
    void closeFund_defaultsToToday() {
        service.closeFund(1L, null);
        assertThat(fund.getClosedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void closeFund_alreadyClosed_throws() {
        fund.setClosedDate(LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> service.closeFund(1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void closeFund_systemFund_throws() {
        fund.setIsSystemFund(true);
        assertThatThrownBy(() -> service.closeFund(1L, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closeFund_futureDate_throws() {
        assertThatThrownBy(() -> service.closeFund(1L, LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(fund.isClosed()).isFalse();
    }
}
