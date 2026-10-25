package com.grupob.bancobanquito.origination;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingDisbursementApplication(
        Integer applicationId,
        Integer customerId,
        BigDecimal approvedAmount,
        BigDecimal effectiveAnnualRate,
        Integer termMonths,
        AmortizationMethod amortizationMethod,
        LocalDate resolutionDate) {
}
