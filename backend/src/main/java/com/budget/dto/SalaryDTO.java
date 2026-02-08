package com.budget.dto;

import com.budget.model.Salary;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SalaryDTO {
    private Long id;
    private String name;
    private BigDecimal regularAmount;
    private BigDecimal federalTax;
    private BigDecimal medicare;
    private BigDecimal socialSecurity;
    private BigDecimal fourOhOneK;
    private BigDecimal extraTaxWithholding;
    private BigDecimal healthSavingsAccount;
    private BigDecimal medicalInsurance;
    private BigDecimal flexibleSpendingAccount;
    private BigDecimal netPay;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static SalaryDTO fromEntity(Salary salary) {
        SalaryDTO dto = new SalaryDTO();
        dto.setId(salary.getId());
        dto.setName(salary.getName());
        dto.setRegularAmount(salary.getRegularAmount());
        dto.setFederalTax(salary.getFederalTax());
        dto.setMedicare(salary.getMedicare());
        dto.setSocialSecurity(salary.getSocialSecurity());
        dto.setFourOhOneK(salary.getFourOhOneK());
        dto.setExtraTaxWithholding(salary.getExtraTaxWithholding());
        dto.setHealthSavingsAccount(salary.getHealthSavingsAccount());
        dto.setMedicalInsurance(salary.getMedicalInsurance());
        dto.setFlexibleSpendingAccount(salary.getFlexibleSpendingAccount());
        dto.setNetPay(calculateNetPay(salary));
        dto.setIsActive(salary.getIsActive());
        dto.setCreatedAt(salary.getCreatedAt());
        return dto;
    }

    private static BigDecimal calculateNetPay(Salary salary) {
        BigDecimal netPay = salary.getRegularAmount();
        netPay = netPay.subtract(salary.getFederalTax());
        netPay = netPay.subtract(salary.getMedicare());
        netPay = netPay.subtract(salary.getSocialSecurity());
        if (salary.getFourOhOneK() != null) {
            netPay = netPay.subtract(salary.getFourOhOneK());
        }
        if (salary.getExtraTaxWithholding() != null) {
            netPay = netPay.subtract(salary.getExtraTaxWithholding());
        }
        if (salary.getHealthSavingsAccount() != null) {
            netPay = netPay.subtract(salary.getHealthSavingsAccount());
        }
        if (salary.getMedicalInsurance() != null) {
            netPay = netPay.subtract(salary.getMedicalInsurance());
        }
        if (salary.getFlexibleSpendingAccount() != null) {
            netPay = netPay.subtract(salary.getFlexibleSpendingAccount());
        }
        return netPay;
    }
}
