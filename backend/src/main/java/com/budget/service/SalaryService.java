package com.budget.service;

import com.budget.dto.CreateSalaryRequest;
import com.budget.dto.SalaryDTO;
import com.budget.dto.UpdateSalaryRequest;
import com.budget.model.Salary;
import com.budget.repository.SalaryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryService {

    private final SalaryRepository salaryRepository;

    public List<SalaryDTO> getAllSalaries() {
        return salaryRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(SalaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public SalaryDTO getSalary(Long id) {
        Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salary not found: " + id));
        return SalaryDTO.fromEntity(salary);
    }

    @Transactional
    public SalaryDTO createSalary(CreateSalaryRequest request) {
        Salary salary = new Salary();
        salary.setName(request.getName());
        salary.setRegularAmount(request.getRegularAmount());
        salary.setFederalTax(request.getFederalTax());
        salary.setMedicare(request.getMedicare());
        salary.setSocialSecurity(request.getSocialSecurity());
        salary.setFourOhOneK(request.getFourOhOneK());
        salary.setExtraTaxWithholding(request.getExtraTaxWithholding());
        salary.setHealthSavingsAccount(request.getHealthSavingsAccount());
        salary.setMedicalInsurance(request.getMedicalInsurance());
        salary.setFlexibleSpendingAccount(request.getFlexibleSpendingAccount());
        salary.setIsActive(true);

        salary = salaryRepository.save(salary);
        return SalaryDTO.fromEntity(salary);
    }

    @Transactional
    public SalaryDTO updateSalary(Long id, UpdateSalaryRequest request) {
        Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salary not found: " + id));

        if (request.getName() != null) {
            salary.setName(request.getName());
        }
        if (request.getRegularAmount() != null) {
            salary.setRegularAmount(request.getRegularAmount());
        }
        if (request.getFederalTax() != null) {
            salary.setFederalTax(request.getFederalTax());
        }
        if (request.getMedicare() != null) {
            salary.setMedicare(request.getMedicare());
        }
        if (request.getSocialSecurity() != null) {
            salary.setSocialSecurity(request.getSocialSecurity());
        }
        // Optional fields - update even if null (allows clearing)
        salary.setFourOhOneK(request.getFourOhOneK());
        salary.setExtraTaxWithholding(request.getExtraTaxWithholding());
        salary.setHealthSavingsAccount(request.getHealthSavingsAccount());
        salary.setMedicalInsurance(request.getMedicalInsurance());
        salary.setFlexibleSpendingAccount(request.getFlexibleSpendingAccount());

        salary = salaryRepository.save(salary);
        return SalaryDTO.fromEntity(salary);
    }

    @Transactional
    public void deleteSalary(Long id) {
        Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salary not found: " + id));

        // Soft delete - just mark as inactive
        salary.setIsActive(false);
        salaryRepository.save(salary);
    }
}
