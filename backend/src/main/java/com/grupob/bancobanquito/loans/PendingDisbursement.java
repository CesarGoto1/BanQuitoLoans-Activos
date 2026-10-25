package com.grupob.bancobanquito.loans;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingDisbursement(
        Integer applicationId,
        Integer customerId,
        String customerNationalId,
        String customerName,
        BigDecimal approvedAmount,
        BigDecimal effectiveAnnualRate,
        Integer termMonths,
        AmortizationMethod amortizationMethod,
        LocalDate resolutionDate) {
}
