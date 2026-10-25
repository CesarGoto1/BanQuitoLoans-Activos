package com.grupob.bancobanquito.loans;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DisbursementResult(
        Integer applicationId,
        String loanNumber,
        BigDecimal netAmount,
        String destinationAccount,
        LocalDate disbursementDate) {
}
