
package com.grupob.bancobanquito.originacion.internal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PoliticaAprobacionPrestamo {

    public void ensureRegistered(String applicationStatus) {

        if (!"REGISTRADA".equals(applicationStatus)) {
            throw new ExcepcionReglaDecisionPrestamo(
                    "La solicitud ya fue aprobada o rechazada."
            );
        }
    }

    public void validateAnnualEffectiveRate(
            BigDecimal teaAsignada,
            BigDecimal maximumAnnualEffectiveRate
    ) {

        if (teaAsignada == null
                || teaAsignada.signum() <= 0) {

            throw new ExcepcionReglaDecisionPrestamo(
                    "La tasa de interés debe ser mayor que cero."
            );
        }

        if (maximumAnnualEffectiveRate == null
                || maximumAnnualEffectiveRate.signum() <= 0) {

            throw new IllegalStateException(
                    "Invalid maximum interest rate configuration."
            );
        }

        if (teaAsignada.compareTo(
                maximumAnnualEffectiveRate) > 0) {

            throw new ExcepcionReglaDecisionPrestamo(
                    "La tasa asignada supera la tasa máxima permitida."
            );
        }
    }

    public BigDecimal validatePaymentCapacity(
            BigDecimal ingresosMensuales,
            BigDecimal gastosMensuales,
            BigDecimal cuotaProyectada,
            BigDecimal maximumPaymentRatio
    ) {

        if (ingresosMensuales == null
                || gastosMensuales == null
                || cuotaProyectada == null) {

            throw new ExcepcionReglaDecisionPrestamo(
                    "Los datos de evaluación están incompletos."
            );
        }

        if (ingresosMensuales.signum() < 0
                || gastosMensuales.signum() < 0
                || cuotaProyectada.signum() <= 0) {

            throw new ExcepcionReglaDecisionPrestamo(
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
                ingresosMensuales.subtract(gastosMensuales);

        if (remainingIncome.signum() <= 0) {
            throw new ExcepcionReglaDecisionPrestamo(
                    "El cliente no dispone de ingresos remanentes suficientes."
            );
        }

        BigDecimal maximumAllowedInstallment =
                remainingIncome
                        .multiply(maximumPaymentRatio)
                        .setScale(2, RoundingMode.HALF_UP);

        if (cuotaProyectada.compareTo(
                maximumAllowedInstallment) > 0) {

            String formattedMaximum =
                    maximumAllowedInstallment
                            .toPlainString()
                            .replace('.', ',');

            throw new ExcepcionReglaDecisionPrestamo(
                    "La cuota proyectada supera el máximo permitido de USD "
                            + formattedMaximum + "."
            );
        }

        return maximumAllowedInstallment;
    }
}
