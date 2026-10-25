package com.grupob.bancobanquito.amortization;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalculatedInstallment(
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal remainingPrincipalBalance) {
}
