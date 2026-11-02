package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class CalculoMoraTest {

    private static final BigDecimal CAPITAL_VENCIDO = new BigDecimal("775.41");
    private static final BigDecimal TASA_NOMINAL = new BigDecimal("0.156042");
    private static final BigDecimal FACTOR_MORA = new BigDecimal("1.10");

    @Test
    void reproduceLaMoraDelDiaNueveDelEjemplo() {
        BigDecimal diaria = CalculoMora.interesDiario(CAPITAL_VENCIDO, TASA_NOMINAL, FACTOR_MORA);

        assertThat(CalculoMora.interesAcumulado(diaria, 9))
                .isEqualByComparingTo("3.3273");
        assertThat(CalculoMora.interesAcumulado(diaria, 9).setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo("3.33");
    }

    @Test
    void reproduceLaMoraDelDiaDiezDelEjemplo() {
        BigDecimal diaria = CalculoMora.interesDiario(CAPITAL_VENCIDO, TASA_NOMINAL, FACTOR_MORA);

        assertThat(CalculoMora.interesAcumulado(diaria, 10).setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo("3.70");
    }

    @Test
    void rechazaDiasNegativos() {
        assertThatThrownBy(() -> CalculoMora.interesAcumulado(BigDecimal.ONE, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
