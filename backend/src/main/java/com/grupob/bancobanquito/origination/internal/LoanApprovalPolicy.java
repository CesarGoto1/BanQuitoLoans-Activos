
package com.grupob.bancobanquito.origination.internal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class LoanApprovalPolicy {

    // Verify that the application can still be resolved.
    public void ensureRegistered(String applicationStatus) {

        if (!"REGISTRADA".equals(applicationStatus)) {
            throw new LoanDecisionRuleException(
                    "La solicitud ya fue aprobada o rechazada."
            );
        }
    }

    // Verify the assigned annual effective interest rate.
    public void validateAnnualEffectiveRate(
            BigDecimal annualEffectiveRate,
            BigDecimal maximumAnnualEffectiveRate
    ) {

        if (annualEffectiveRate == null
                || annualEffectiveRate.signum() <= 0) {

            throw new LoanDecisionRuleException(
                    "La tasa de interés debe ser mayor que cero."
            );
        }

        if (maximumAnnualEffectiveRate == null
                || maximumAnnualEffectiveRate.signum() <= 0) {

            throw new IllegalStateException(
                    "Invalid maximum interest rate configuration."
            );
        }

        if (annualEffectiveRate.compareTo(
                maximumAnnualEffectiveRate) > 0) {

            throw new LoanDecisionRuleException(
                    "La tasa asignada supera la tasa máxima permitida."
            );
        }
    }

    // Validate the projected installment against payment capacity.
    public BigDecimal validatePaymentCapacity(
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses,
            BigDecimal projectedInstallment,
            BigDecimal maximumPaymentRatio
    ) {

        if (monthlyIncome == null
                || monthlyExpenses == null
                || projectedInstallment == null) {

            throw new LoanDecisionRuleException(
                    "Los datos de evaluación están incompletos."
            );
        }

        if (monthlyIncome.signum() < 0
                || monthlyExpenses.signum() < 0
                || projectedInstallment.signum() <= 0) {

            throw new LoanDecisionRuleException(
                    "Los valores de evaluación no son válidos."
            );
        }

        if (maximumPaymentRatio == null
                || maximumPaymentRatio.signum() <= 0
                || maximumPaymentRatio.compareTo(BigDecimal.ONE) > 0) {

            throw new IllegalStateException(
                    "Invalid payment capacity configuration."
            );
        }

        BigDecimal remainingIncome =
                monthlyIncome.subtract(monthlyExpenses);

        if (remainingIncome.signum() <= 0) {
            throw new LoanDecisionRuleException(
                    "El cliente no dispone de ingresos remanentes suficientes."
            );
        }

        BigDecimal maximumAllowedInstallment =
                remainingIncome
                        .multiply(maximumPaymentRatio)
                        .setScale(2, RoundingMode.HALF_UP);

        if (projectedInstallment.compareTo(
                maximumAllowedInstallment) > 0) {

            String formattedMaximum =
                    maximumAllowedInstallment
                            .toPlainString()
                            .replace('.', ',');

            throw new LoanDecisionRuleException(
                    "La cuota proyectada supera el máximo permitido de USD "
                            + formattedMaximum + "."
            );
        }

        return maximumAllowedInstallment;
    }
}
