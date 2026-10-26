
package com.grupob.bancobanquito.originacion.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PeticionAprobarPrestamo(

        @JsonProperty("teaAsignada")
        @NotNull(message = "La tasa de interés es obligatoria")
        @Positive(message = "La tasa de interés debe ser mayor que cero")
        @Digits(
                integer = 1,
                fraction = 4,
                message = "La tasa debe tener máximo cuatro decimales"
        )
        BigDecimal teaAsignada

) {
}
