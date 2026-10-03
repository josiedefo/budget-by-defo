package com.budget.dto;

import com.budget.model.FundGoalType;
import com.budget.model.FundStatus;
import com.budget.model.SavingsFund;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SavingsFundDTO {
    private Long id;
    private String name;
    private FundGoalType goalType;
    private BigDecimal balance;
    private BigDecimal targetAmount;
    private LocalDate deadline;
    private BigDecimal ceiling;
    private LocalDate payoutDate;
    private BigDecimal payoutAmount;
    private Boolean isSystemFund;
    private Boolean isActive;
    private LocalDateTime createdAt;
    // Computed fields
    private BigDecimal remaining;
    private Integer progressPercent;
    private FundStatus status;
    private BigDecimal ytdSpent;
    private LocalDate closedDate;
    private Boolean isClosed;
    // Lifetime figures, populated for closed funds so progress survives the money being spent
    private BigDecimal totalSaved;
    private BigDecimal totalUsed;
    private BigDecimal releasedAmount;

    public static SavingsFundDTO fromEntity(SavingsFund fund) {
        return fromEntityWithYtd(fund, null);
    }

    public static SavingsFundDTO fromEntityWithYtd(SavingsFund fund, BigDecimal ytdSpent) {
        return fromEntityWithStats(fund, ytdSpent, null, null, null);
    }

    public static SavingsFundDTO fromEntityWithStats(SavingsFund fund, BigDecimal ytdSpent,
                                                     BigDecimal totalSaved, BigDecimal totalUsed,
                                                     BigDecimal releasedAmount) {
        SavingsFundDTO dto = new SavingsFundDTO();
        dto.setTotalSaved(totalSaved);
        dto.setTotalUsed(totalUsed);
        dto.setReleasedAmount(releasedAmount);
        dto.setId(fund.getId());
        dto.setName(fund.getName());
        dto.setGoalType(fund.getGoalType());
        dto.setBalance(fund.getBalance());
        dto.setTargetAmount(fund.getTargetAmount());
        dto.setDeadline(fund.getDeadline());
        dto.setCeiling(fund.getCeiling());
        dto.setPayoutDate(fund.getPayoutDate());
        dto.setPayoutAmount(fund.getPayoutAmount());
        dto.setIsSystemFund(fund.getIsSystemFund());
        dto.setIsActive(fund.getIsActive());
        dto.setCreatedAt(fund.getCreatedAt());
        dto.setYtdSpent(ytdSpent);
        dto.setClosedDate(fund.getClosedDate());
        dto.setIsClosed(fund.isClosed());
        dto.computeDerivedFields();
        return dto;
    }

    private void computeDerivedFields() {
        // A closed fund has been saved for and spent; judge it by what was saved over its life,
        // not by the (now zero) balance.
        if (Boolean.TRUE.equals(isClosed) && totalSaved != null
                && (goalType == FundGoalType.TARGET || goalType == FundGoalType.TARGET_WITH_DEADLINE
                    || goalType == FundGoalType.SPEND_DOWN)
                && targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            remaining = targetAmount.subtract(totalSaved).max(BigDecimal.ZERO);
            progressPercent = Math.min(100, totalSaved.multiply(BigDecimal.valueOf(100))
                    .divide(targetAmount, 0, RoundingMode.FLOOR).intValue());
            status = totalSaved.compareTo(targetAmount) >= 0 ? FundStatus.COMPLETE : FundStatus.CLOSED;
            return;
        }
        switch (goalType) {
            case TARGET:
            case TARGET_WITH_DEADLINE:
            case SPEND_DOWN:
                if (targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0) {
                    remaining = targetAmount.subtract(balance).max(BigDecimal.ZERO);
                    int pct = balance.multiply(BigDecimal.valueOf(100))
                            .divide(targetAmount, 0, RoundingMode.FLOOR)
                            .intValue();
                    progressPercent = Math.min(pct, 100);
                    status = balance.compareTo(targetAmount) >= 0 ? FundStatus.COMPLETE : FundStatus.IN_PROGRESS;
                } else {
                    remaining = null;
                    progressPercent = null;
                    status = FundStatus.IN_PROGRESS;
                }
                break;
            case SPEND_AS_YOU_GO:
                if (ceiling != null && ytdSpent != null) {
                    status = ytdSpent.compareTo(ceiling) >= 0 ? FundStatus.AT_LIMIT : FundStatus.OK;
                } else {
                    status = FundStatus.OK;
                }
                remaining = null;
                progressPercent = null;
                break;
            case NO_GOAL:
            default:
                remaining = null;
                progressPercent = null;
                status = null;
                break;
        }
    }
}
