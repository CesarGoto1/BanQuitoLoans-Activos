package com.grupob.bancobanquito.acceso.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

record PeticionInicioSesionAnalista(

        @NotBlank(message = "La cédula es obligatoria.")
        @Pattern(regexp = "^[0-9]{10}$", message = "La cédula debe tener 10 dígitos numéricos.")
        String cedula) {
}
