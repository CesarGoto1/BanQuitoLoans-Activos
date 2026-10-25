package com.grupob.bancobanquito.amortization;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaAmortizacion(
        int numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal valorCapital,
        BigDecimal valorInteres,
        BigDecimal valorSeguro,
        BigDecimal totalCuota,
        BigDecimal saldoCapitalPosterior) {
}
