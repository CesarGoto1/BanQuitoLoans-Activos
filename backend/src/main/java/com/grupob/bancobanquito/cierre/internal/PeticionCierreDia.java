package com.grupob.bancobanquito.cierre.internal;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

record PeticionCierreDia(
        @NotNull(message = "Indica la fecha de negocio que se va a cerrar.")
        LocalDate fechaNegocio) {
}
