package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.amortization.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class AmortizacionServiceTest {
    private final AmortizacionService servicio = new AmortizacionServiceImpl();

    private TablaAmortizacion generar(SistemaAmortizacion sistema) {
        return servicio.generarTabla(new CondicionesAmortizacion(new BigDecimal("10000.00"), 12,
                new BigDecimal("0.1677"), new BigDecimal("6.00"), LocalDate.of(2026, 10, 31), sistema));
    }

    @Test
    void reproducirCadaCuotaFrancesaDelEjemplo86() {
        var tabla = generar(SistemaAmortizacion.FRANCES);
        String[][] esperado = {
            {"775.41", "130.03", "9224.59"}, {"785.49", "119.95", "8439.10"},
            {"795.70", "109.74", "7643.40"}, {"806.05", "99.39", "6837.35"},
            {"816.53", "88.91", "6020.82"}, {"827.15", "78.29", "5193.67"},
            {"837.90", "67.54", "4355.77"}, {"848.80", "56.64", "3506.97"},
            {"859.84", "45.60", "2647.13"}, {"871.02", "34.42", "1776.11"},
            {"882.34", "23.10", "893.77"}, {"893.77", "11.62", "0.00"}
        };
        assertThat(tabla.cuotas()).hasSize(12);
        for (int indice = 0; indice < esperado.length; indice++) {
            var cuota = tabla.cuotas().get(indice);
            assertThat(cuota.numeroCuota()).isEqualTo(indice + 1);
            assertThat(cuota.valorCapital()).isEqualByComparingTo(esperado[indice][0]);
            assertThat(cuota.valorInteres()).isEqualByComparingTo(esperado[indice][1]);
            assertThat(cuota.saldoCapitalPosterior()).isEqualByComparingTo(esperado[indice][2]);
            assertThat(cuota.valorSeguro()).isEqualByComparingTo("6.00");
            assertThat(cuota.totalCuota()).isEqualByComparingTo(indice == 11 ? "911.39" : "911.44");
            assertThat(cuota.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 10, 31).plusMonths(indice + 1));
        }
        assertThat(tabla.totalIntereses()).isEqualByComparingTo("865.23");
    }

    @Test
    void reproducirSistemaAlemanDelEjemplo86() {
        var tabla = generar(SistemaAmortizacion.ALEMAN);
        assertThat(tabla.cuotas().get(0).totalCuota()).isEqualByComparingTo("969.36");
        assertThat(tabla.cuotas().get(1).totalCuota()).isEqualByComparingTo("958.53");
        assertThat(tabla.cuotas().get(2).totalCuota()).isEqualByComparingTo("947.69");
        assertThat(tabla.cuotas().getLast().valorCapital()).isEqualByComparingTo("833.37");
        assertThat(tabla.cuotas().getLast().totalCuota()).isEqualByComparingTo("850.21");
        assertThat(tabla.totalIntereses()).isEqualByComparingTo("845.23");
    }

    @ParameterizedTest
    @EnumSource(SistemaAmortizacion.class)
    void conservarCapitalEnMontosYPlazosDistintos(SistemaAmortizacion sistema) {
        for (String monto : new String[]{"500.00", "500.01", "12345.67", "30000.00"}) {
            for (int plazo : new int[]{6, 12, 37, 60}) {
                var tabla = servicio.generarTabla(new CondicionesAmortizacion(new BigDecimal(monto), plazo,
                        new BigDecimal("0.1550"), new BigDecimal("0.30"), LocalDate.of(2026, 10, 31), sistema));
                assertThat(tabla.cuotas().stream().map(CuotaAmortizacion::valorCapital).reduce(BigDecimal.ZERO, BigDecimal::add))
                        .isEqualByComparingTo(monto);
                assertThat(tabla.cuotas().getLast().saldoCapitalPosterior()).isZero();
                assertThat(tabla.cuotas()).allSatisfy(cuota -> {
                    assertThat(cuota.valorCapital()).isNotNegative();
                    assertThat(cuota.saldoCapitalPosterior()).isNotNegative();
                    assertThat(cuota.totalCuota()).isEqualByComparingTo(cuota.valorCapital().add(cuota.valorInteres()).add(cuota.valorSeguro()));
                });
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"2027-01-29,2027-02-28,2027-03-29", "2027-01-30,2027-02-28,2027-03-30",
            "2027-01-31,2027-02-28,2027-03-31", "2028-01-31,2028-02-29,2028-03-31"})
    void conservarDiaAnclaInclusoEnFebrero(LocalDate inicio, LocalDate primero, LocalDate segundo) {
        var tabla = servicio.generarTabla(new CondicionesAmortizacion(new BigDecimal("500.00"), 6,
                new BigDecimal("0.1550"), new BigDecimal("0.30"), inicio, SistemaAmortizacion.FRANCES));
        assertThat(tabla.cuotas().get(0).fechaVencimiento()).isEqualTo(primero);
        assertThat(tabla.cuotas().get(1).fechaVencimiento()).isEqualTo(segundo);
    }

    @Test
    void admitirTasaCeroSinDividirPorCero() {
        var tabla = servicio.generarTabla(new CondicionesAmortizacion(new BigDecimal("1000.00"), 6,
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 10, 31), SistemaAmortizacion.FRANCES));
        assertThat(tabla.totalIntereses()).isZero();
        assertThat(tabla.cuotas().getLast().valorCapital()).isEqualByComparingTo("166.65");
    }
}
