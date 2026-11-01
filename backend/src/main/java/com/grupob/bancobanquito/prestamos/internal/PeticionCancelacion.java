package com.grupob.bancobanquito.prestamos.internal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

record PeticionCancelacion(

        @NotNull(message = "El valor recibido es obligatorio.")
        @DecimalMin(value = "0.01", message = "El valor recibido debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El valor recibido tiene máximo dos decimales.")
        BigDecimal montoRecibido) {
}
