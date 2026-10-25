
package com.grupob.bancobanquito.origination.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectLoanRequest(

        @JsonProperty("motivoRechazo")
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(
                max = 200,
                message = "El motivo no puede superar los 200 caracteres"
        )
        String rejectionReason

) {
}
