package com.grupob.bancobanquito.liabilities.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

record LiabilitiesDisbursement(
        String version,
        String loanNumber,
        String customerNationalId,
        String destinationAccount,
        BigDecimal approvedAmount,
        BigDecimal solcaContribution,
        BigDecimal netAmount,
        LocalDate disbursementDate,
        String reference) {
}
