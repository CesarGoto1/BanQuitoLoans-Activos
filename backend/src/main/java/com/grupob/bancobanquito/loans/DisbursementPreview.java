package com.grupob.bancobanquito.loans;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import com.grupob.bancobanquito.liabilities.Account;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DisbursementPreview(
        Integer applicationId,
        String customerNationalId,
        String customerName,
        BigDecimal approvedAmount,
        BigDecimal effectiveAnnualRate,
        Integer termMonths,
        AmortizationMethod amortizationMethod,
        BigDecimal monthlyInsurance,
        BigDecimal solcaContribution,
        BigDecimal netAmount,
        LocalDate disbursementDate,
        List<Account> accounts) {
}
