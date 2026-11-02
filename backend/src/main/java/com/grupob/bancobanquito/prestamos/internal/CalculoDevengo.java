package com.grupob.bancobanquito.prestamos.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class CalculoDevengo {

    static final int BASE_DIAS_COMERCIAL = 360;
    private static final int ESCALA_DIARIA = 4;

    private CalculoDevengo() {
    }

    static BigDecimal interesDiario(BigDecimal saldoCapital, BigDecimal tasaNominalAnual) {
        return interesDiario(saldoCapital, tasaNominalAnual, BASE_DIAS_COMERCIAL);
    }

    static BigDecimal interesDiario(BigDecimal saldoCapital, BigDecimal tasaNominalAnual, int baseDias) {
        validarBaseDias(baseDias);
        return saldoCapital.multiply(tasaNominalAnual)
                .divide(BigDecimal.valueOf(baseDias), ESCALA_DIARIA, RoundingMode.HALF_UP);
    }

    static BigDecimal interesAcumulado(BigDecimal interesDiario, long dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("Los días no pueden ser negativos.");
        }
        return interesDiario.multiply(BigDecimal.valueOf(dias));
    }

    static long diasDevengamiento(LocalDate fechaDesembolso, LocalDate fechaProceso) {
        if (fechaProceso.isBefore(fechaDesembolso)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(fechaDesembolso, fechaProceso);
    }

    private static void validarBaseDias(int baseDias) {
        if (baseDias <= 0) {
            throw new IllegalArgumentException("La base de días debe ser positiva.");
        }
    }
}
