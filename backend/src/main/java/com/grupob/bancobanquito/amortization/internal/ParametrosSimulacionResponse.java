package com.grupob.bancobanquito.amortization.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

record ParametrosSimulacionResponse(
        LocalDate fechaNegocio, BigDecimal montoMinimo, BigDecimal montoMaximo,
        int plazoMinimo, int plazoMaximo, BigDecimal teaReferencial,
        BigDecimal factorSeguroMensual, BigDecimal factorSolca) {
}
