package com.grupob.bancobanquito.loans;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenCuota(
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal valorCapital,
        BigDecimal valorInteres,
        BigDecimal valorSeguro,
        BigDecimal valorCuota,
        BigDecimal saldoCapitalPendiente,
        String estadoCuota) {
}
