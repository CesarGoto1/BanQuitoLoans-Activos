package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

record LoanApplicationRequest(
        @NotNull(message = "El cliente es obligatorio.")
        Integer customerId,
        @NotNull(message = "Ingresa el monto que necesitas.")
        @Positive(message = "El monto debe ser mayor a cero.")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales.")
        BigDecimal amount,
        @NotNull(message = "Ingresa el plazo en meses.")
        Integer termMonths,
        @NotNull(message = "Elige el sistema de amortización.")
        AmortizationMethod amortizationMethod,
        @NotNull(message = "Elige el destino del crédito.")
        CreditPurpose purpose,
        @NotNull(message = "Ingresa tus ingresos mensuales.")
        @Positive(message = "Los ingresos mensuales deben ser mayores a cero.")
        @Digits(integer = 8, fraction = 2, message = "Los ingresos admiten hasta 2 decimales.")
        BigDecimal monthlyIncome,
        @NotNull(message = "Ingresa tus gastos mensuales.")
        @PositiveOrZero(message = "Los gastos mensuales no pueden ser negativos.")
        @Digits(integer = 8, fraction = 2, message = "Los gastos admiten hasta 2 decimales.")
        BigDecimal monthlyExpenses) {
}
