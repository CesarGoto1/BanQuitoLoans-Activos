package com.grupob.bancobanquito.loans;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanSummary(
        String loanNumber,
        Integer applicationId,
        String customerNationalId,
        BigDecimal approvedAmount,
        BigDecimal effectiveAnnualRate,
        Integer termMonths,
        AmortizationMethod amortizationMethod,
        LocalDate disbursementDate,
        BigDecimal principalBalance,
        Integer daysPastDue,
        PortfolioStatus portfolioStatus) {
}
