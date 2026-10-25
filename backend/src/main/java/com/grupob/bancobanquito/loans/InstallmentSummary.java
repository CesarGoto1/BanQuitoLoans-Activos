package com.grupob.bancobanquito.loans;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentSummary(
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal insuranceAmount,
        BigDecimal totalAmount,
        BigDecimal remainingPrincipalBalance,
        String status) {
}
