package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CalculoDevengoTest {

    @Test
    void calculaElInteresDiarioConBaseComercialDe360Dias() {
        BigDecimal resultado = CalculoDevengo.interesDiario(
                new BigDecimal("9224.59"), new BigDecimal("0.156042"));

        assertThat(resultado).isEqualByComparingTo("3.9984");
    }

    @Test
    void comienzaADevengarElDiaSiguienteAlDesembolso() {
        LocalDate desembolso = LocalDate.of(2026, 10, 31);

        assertThat(CalculoDevengo.diasDevengamiento(desembolso, desembolso)).isZero();
        assertThat(CalculoDevengo.diasDevengamiento(desembolso, desembolso.plusDays(1))).isEqualTo(1);
        assertThat(CalculoDevengo.diasDevengamiento(desembolso, desembolso.plusDays(15))).isEqualTo(15);
    }

    @Test
    void acumulaElInteresSinPerderLaPrecisionDiaria() {
        assertThat(CalculoDevengo.interesAcumulado(new BigDecimal("3.9984"), 15))
                .isEqualByComparingTo("59.9760");
    }

    @Test
    void rechazaBaseDeDiasNoPositiva() {
        assertThatThrownBy(() -> CalculoDevengo.interesDiario(
                BigDecimal.ONE, BigDecimal.ONE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
