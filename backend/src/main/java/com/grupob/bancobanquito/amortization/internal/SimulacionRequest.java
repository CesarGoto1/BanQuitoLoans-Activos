package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.amortization.SistemaAmortizacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

record SimulacionRequest(
        @NotNull(message = "El monto es obligatorio.")
        @Positive(message = "El monto debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 8 enteros y 2 decimales.")
        @Schema(description = "Monto solicitado en USD. Rango vigente PAR-01 a PAR-02.", example = "10000.00")
        BigDecimal montoSolicitado,
        @NotNull(message = "El plazo es obligatorio.")
        @Positive(message = "El plazo debe ser mayor que cero.")
        @Schema(description = "Meses completos. Rango vigente PAR-03 a PAR-04.", example = "12")
        Integer plazoMeses,
        @NotNull(message = "El sistema de amortización es obligatorio.")
        @Schema(example = "FRANCES") SistemaAmortizacion sistemaAmortizacion) {
}
