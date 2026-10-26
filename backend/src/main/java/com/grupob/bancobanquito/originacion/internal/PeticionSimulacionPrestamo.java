package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

record PeticionSimulacionPrestamo(

        @NotNull(message = "Ingresa el monto que necesitas.")
        @Positive(message = "El monto debe ser mayor a cero.")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales.")
        BigDecimal monto,

        @NotNull(message = "Ingresa el plazo en meses.")
        Integer plazoMeses,

        @NotNull(message = "Elige el sistema de amortización.")
        MetodoAmortizacion metodoAmortizacion) {
}
