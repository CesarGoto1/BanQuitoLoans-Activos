package com.grupob.bancobanquito.amortization;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CondicionesAmortizacion(
        BigDecimal montoAprobado,
        int plazoMeses,
        BigDecimal teaAsignada,
        BigDecimal seguroMensual,
        LocalDate fechaDesembolso,
        SistemaAmortizacion sistemaAmortizacion) {
}
