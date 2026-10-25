package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.internal.LoanApplication.AmortizationSystem;
import com.grupob.bancobanquito.origination.internal.LoanApplication.CreditPurpose;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record RegisterLoanApplicationRequest(
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal requestedAmount,
        @NotNull @Positive Integer termMonths,
        @NotNull AmortizationSystem amortizationSystem,
        @NotNull CreditPurpose creditPurpose,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal monthlyIncome,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal monthlyExpenses) {
}
