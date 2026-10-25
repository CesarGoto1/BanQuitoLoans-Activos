package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

record LoanApplicationResponse(
        Integer applicationId,
        Integer customerId,
        BigDecimal amount,
        Integer termMonths,
        AmortizationMethod amortizationMethod,
        CreditPurpose purpose,
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        LocalDate applicationDate,
        LocalDateTime registeredAt,
        BigDecimal referenceInstallment,
        BigDecimal assignedRate,
        LoanApplicationStatus status,
        LocalDate resolutionDate,
        String rejectionReason) {

    static LoanApplicationResponse from(LoanApplication application) {
        return new LoanApplicationResponse(application.getId(), application.getCustomerId(),
                application.getRequestedAmount(), application.getTermMonths(), application.getAmortizationMethod(),
                application.getPurpose(), application.getMonthlyIncome(), application.getMonthlyExpenses(),
                application.getApplicationDate(), application.getRegisteredAt(), application.getReferenceInstallment(),
                application.getAssignedRate(), application.getStatus(), application.getResolutionDate(),
                application.getRejectionReason());
    }
}
