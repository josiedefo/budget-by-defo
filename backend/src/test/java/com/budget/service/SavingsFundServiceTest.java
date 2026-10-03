package com.budget.service;

import com.budget.dto.SavingsFundDTO;
import com.budget.model.FundGoalType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
