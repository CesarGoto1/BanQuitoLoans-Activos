package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

record PeticionSolicitud(

        @NotNull(message = "Ingresa el monto que necesitas.")
        @Positive(message = "El monto debe ser mayor a cero.")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales.")
        BigDecimal monto,

        @NotNull(message = "Ingresa el plazo en meses.")
        Integer plazoMeses,

        @NotNull(message = "Elige el sistema de amortización.")
        MetodoAmortizacion metodoAmortizacion,

        @NotNull(message = "Elige el destino del crédito.")
        DestinoCredito destinoCredito,

        @NotNull(message = "Ingresa tus ingresos mensuales.")
        @Positive(message = "Los ingresos mensuales deben ser mayores a cero.")
        @Digits(integer = 8, fraction = 2, message = "Los ingresos admiten hasta 2 decimales.")
        BigDecimal ingresosMensuales,

        @NotNull(message = "Ingresa tus gastos mensuales.")
        @PositiveOrZero(message = "Los gastos mensuales no pueden ser negativos.")
        @Digits(integer = 8, fraction = 2, message = "Los gastos admiten hasta 2 decimales.")
        BigDecimal gastosMensuales) {
}
