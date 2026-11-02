package com.grupob.bancobanquito.pagos.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupob.bancobanquito.prestamos.CuotaPorPagar;
import com.grupob.bancobanquito.prestamos.ImputacionCuota;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraPrelacionTest {

    private static final LocalDate FECHA_PAGO = LocalDate.of(2026, 12, 10);

    private final CalculadoraPrelacion calculadora = new CalculadoraPrelacion();

    private final List<CuotaPorPagar> cuotas = List.of(
            cuota(1, 1, "2026-11-30", "6.00", "3.33", "130.03", "775.41"),
            cuota(2, 2, "2026-12-31", "6.00", "0.00", "119.95", "785.49"),
            cuota(3, 3, "2027-01-31", "6.00", "0.00", "109.74", "795.70"));

    @Test
    void pagoCompletoDeLaCuotaVencidaDelEjemplo() {
        DistribucionPago distribucion = calculadora.distribuir(cuotas, monto("914.77"), FECHA_PAGO);

        assertThat(distribucion.imputaciones()).containsExactly(
                imputacion(1, "6.00", "3.33", "130.03", "775.41"));
        assertThat(distribucion.excedente()).isEqualByComparingTo("0");
        assertThat(distribucion.cubreTodaLaDeuda()).isFalse();
    }

    @Test
    void pagoParcialDelEjemploDejaCapitalPendiente() {
        DistribucionPago distribucion = calculadora.distribuir(cuotas, monto("500.00"), FECHA_PAGO);

        assertThat(distribucion.imputaciones()).containsExactly(
                imputacion(1, "6.00", "3.33", "130.03", "360.64"));
        assertThat(monto("775.41").subtract(distribucion.capital())).isEqualByComparingTo("414.77");
    }

    @Test
    void elExcedenteDeLaCuotaVencidaPasaALaCuotaEnCurso() {
        DistribucionPago distribucion = calculadora.distribuir(cuotas, monto("1000.00"), FECHA_PAGO);

        assertThat(distribucion.imputaciones()).containsExactly(
                imputacion(1, "6.00", "3.33", "130.03", "775.41"),
                imputacion(2, "6.00", "0.00", "79.23", "0.00"));
        assertThat(distribucion.anticipado()).isEqualByComparingTo("0");
        assertThat(distribucion.totalAplicado()).isEqualByComparingTo("1000.00");
    }

    @Test
    void loQueSobraTrasLaCuotaEnCursoEsPagoAnticipado() {
        DistribucionPago distribucion = calculadora.distribuir(cuotas, monto("2000.00"), FECHA_PAGO);

        assertThat(distribucion.capital()).isEqualByComparingTo("1560.90");
        assertThat(distribucion.anticipado()).isEqualByComparingTo("173.79");
        assertThat(distribucion.imputaciones().get(2)).isEqualTo(imputacion(3, "6.00", "0.00", "109.74", "58.05"));
        assertThat(distribucion.totalAplicado()).isEqualByComparingTo("2000.00");
    }

    @Test
    void elSobrepagoCubreTodaLaDeudaYDevuelveElExcedente() {
        DistribucionPago distribucion = calculadora.distribuir(cuotas, monto("3000.00"), FECHA_PAGO);

        assertThat(distribucion.cubreTodaLaDeuda()).isTrue();
        assertThat(distribucion.totalAplicado()).isEqualByComparingTo("2737.65");
        assertThat(distribucion.excedente()).isEqualByComparingTo("262.35");
    }

    @Test
    void unPagoExactoDeLaDeudaNoDejaExcedente() {
        DistribucionPago distribucion = calculadora.distribuir(cuotas, monto("2737.65"), FECHA_PAGO);

        assertThat(distribucion.cubreTodaLaDeuda()).isTrue();
        assertThat(distribucion.excedente()).isEqualByComparingTo("0");
    }

    private static CuotaPorPagar cuota(int id, int numero, String vencimiento, String seguro, String mora,
                                       String interes, String capital) {
        return new CuotaPorPagar(id, numero, LocalDate.parse(vencimiento), monto(seguro), monto(mora),
                monto(interes), monto(capital));
    }

    private static ImputacionCuota imputacion(int idCuota, String seguro, String mora, String interes,
                                              String capital) {
        return new ImputacionCuota(idCuota, monto(seguro), monto(mora), monto(interes), monto(capital));
    }

    private static BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }
}
