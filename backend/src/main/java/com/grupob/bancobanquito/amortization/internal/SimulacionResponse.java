package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.amortization.SistemaAmortizacion;
import com.grupob.bancobanquito.amortization.TablaAmortizacion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

record SimulacionResponse(
        LocalDate fechaNegocio,
        BigDecimal montoSolicitado,
        int plazoMeses,
        SistemaAmortizacion sistemaAmortizacion,
        @Schema(description = "Fracción anual: 0.1550 equivale a 15.50 %.") BigDecimal teaReferencial,
        BigDecimal seguroMensual,
        BigDecimal contribucionSolca,
        BigDecimal montoNeto,
        BigDecimal primeraCuota,
        BigDecimal ultimaCuota,
        @Schema(description = "Intereses más seguros más SOLCA, sin incluir capital.") BigDecimal costoTotalCredito,
        TablaAmortizacion tablaAmortizacion) {
}
