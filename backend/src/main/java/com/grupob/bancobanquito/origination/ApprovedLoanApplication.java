package com.grupob.bancobanquito.origination;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import java.math.BigDecimal;

public record ApprovedLoanApplication(
        Integer applicationId,
        Integer customerId,
        BigDecimal approvedAmount,
        BigDecimal effectiveAnnualRate,
        Integer termMonths,
        AmortizationMethod amortizationMethod) {
}
