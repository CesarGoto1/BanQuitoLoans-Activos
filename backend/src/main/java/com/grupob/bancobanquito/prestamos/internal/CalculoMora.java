package com.grupob.bancobanquito.prestamos.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CalculoMora {

    private static final int ESCALA_DIARIA = 4;

    private CalculoMora() {
    }

    static BigDecimal interesDiario(BigDecimal capitalVencido, BigDecimal tasaNominalAnual,
                                    BigDecimal factorMora) {
        return interesDiario(capitalVencido, tasaNominalAnual, factorMora,
                CalculoDevengo.BASE_DIAS_COMERCIAL);
    }

    static BigDecimal interesDiario(BigDecimal capitalVencido, BigDecimal tasaNominalAnual,
                                    BigDecimal factorMora, int baseDias) {
        if (baseDias <= 0) {
            throw new IllegalArgumentException("La base de días debe ser positiva.");
        }
        return capitalVencido.multiply(tasaNominalAnual).multiply(factorMora)
                .divide(BigDecimal.valueOf(baseDias), ESCALA_DIARIA, RoundingMode.HALF_UP);
    }

    static BigDecimal interesAcumulado(BigDecimal interesDiario, long dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("Los días no pueden ser negativos.");
        }
        return interesDiario.multiply(BigDecimal.valueOf(dias));
    }
}
