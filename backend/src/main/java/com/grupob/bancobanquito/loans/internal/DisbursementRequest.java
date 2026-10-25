package com.grupob.bancobanquito.loans.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record DisbursementRequest(
        @NotNull(message = "El número de solicitud es obligatorio.")
        Integer applicationId,
        @NotBlank(message = "Elige la cuenta de destino.")
        @Size(max = 20, message = "La cuenta de destino tiene máximo 20 caracteres.")
        String destinationAccount) {
}
