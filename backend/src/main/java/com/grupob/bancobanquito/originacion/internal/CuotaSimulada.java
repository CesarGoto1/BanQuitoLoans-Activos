package com.grupob.bancobanquito.originacion.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

record CuotaSimulada(
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal capital,
        BigDecimal interes,
        BigDecimal seguro,
        BigDecimal total,
        BigDecimal remainingPrincipalBalance) {
}
