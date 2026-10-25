package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

record LoanSimulationRequest(
        @NotNull(message = "Ingresa el monto que necesitas.")
        @Positive(message = "El monto debe ser mayor a cero.")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales.")
        BigDecimal amount,
        @NotNull(message = "Ingresa el plazo en meses.")
        Integer termMonths,
        @NotNull(message = "Elige el sistema de amortización.")
        AmortizationMethod amortizationMethod) {
}
