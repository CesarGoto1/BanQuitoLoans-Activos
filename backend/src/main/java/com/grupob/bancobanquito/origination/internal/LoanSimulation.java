package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import java.math.BigDecimal;
import java.util.List;

record LoanSimulation(
        BigDecimal amount,
        Integer termMonths,
        AmortizationMethod amortizationMethod,
        BigDecimal effectiveAnnualRate,
        BigDecimal nominalAnnualRate,
        BigDecimal monthlyInsurance,
        BigDecimal solcaContribution,
        BigDecimal netAmount,
        BigDecimal firstInstallment,
        BigDecimal lastInstallment,
        BigDecimal totalInterest,
        BigDecimal totalInsurance,
        BigDecimal totalCost,
        List<SimulatedInstallment> installments) {

    BigDecimal highestInstallment() {
        return installments.stream().map(SimulatedInstallment::total).max(BigDecimal::compareTo).orElseThrow();
    }
}
