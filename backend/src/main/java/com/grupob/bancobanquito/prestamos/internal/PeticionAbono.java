package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

record PeticionAbono(

        @NotNull(message = "El valor del abono es obligatorio.")
        @DecimalMin(value = "0.01", message = "El valor del abono debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El valor del abono tiene máximo dos decimales.")
        BigDecimal monto,

        @NotNull(message = "Elige la modalidad: DISMINUIR_CUOTA o DISMINUIR_PLAZO.")
        ModalidadAbono modalidad) {
}
