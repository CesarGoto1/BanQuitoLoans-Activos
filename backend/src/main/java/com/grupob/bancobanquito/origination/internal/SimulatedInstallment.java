package com.grupob.bancobanquito.origination.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

record SimulatedInstallment(
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal insurance,
        BigDecimal total,
        BigDecimal remainingPrincipalBalance) {
}
