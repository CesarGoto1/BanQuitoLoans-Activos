package com.grupob.bancobanquito.liabilities;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DisbursementInstruction(
        String loanNumber,
        String customerNationalId,
        String destinationAccount,
        BigDecimal approvedAmount,
        BigDecimal solcaContribution,
        BigDecimal netAmount,
        LocalDate disbursementDate) {
}
