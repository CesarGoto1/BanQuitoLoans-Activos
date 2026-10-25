package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.internal.LoanApplication.ApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanApplicationResponse(
        Integer id,
        ApplicationStatus status,
        LocalDate applicationDate,
        BigDecimal referenceEffectiveAnnualRate,
        BigDecimal projectedInstallment,
        BigDecimal monthlySurplus,
        BigDecimal maxAllowedInstallment,
        BigDecimal surplusPercentage,
        String rejectionReason) {
}
