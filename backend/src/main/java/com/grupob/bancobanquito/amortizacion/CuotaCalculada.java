package com.grupob.bancobanquito.amortizacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaCalculada(
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal capital,
        BigDecimal interes,
        BigDecimal remainingPrincipalBalance) {
}
