package com.grupob.bancobanquito.reportes.internal;

import static com.grupob.bancobanquito.reportes.internal.ServiceReportesCarteraTest.operacion;
import static com.grupob.bancobanquito.reportes.internal.ServiceReportesCarteraTest.tramo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.reportes.CorteCartera;
import com.grupob.bancobanquito.reportes.ResultadoRollRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraRollRateTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);
    private final ServiceReportesCarteraImpl servicio = new ServiceReportesCarteraImpl(org.mockito.Mockito.mock(
            com.grupob.bancobanquito.cobranza.ServicioFotografiaCartera.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.cierre.ServiceCierreDia.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.prestamos.ConsultaPrestamoService.class));
    private final List<CorteCartera.Tramo> tramos = List.of(
            tramo(1, 1, EstadoCartera.POR_VENCER), tramo(2, 2, EstadoCartera.VENCIDA));

    @Test
    void usaSaldosInicialesIncluyeCanceladasEIgnoraAltasPosteriores() {
        var inicial = new CorteCartera(FECHA, List.of(
                operacion(10, 1, EstadoCartera.POR_VENCER, "100.00", "1.00"),
                operacion(20, 1, EstadoCartera.POR_VENCER, "200.00", "2.00"),
                operacion(30, 2, EstadoCartera.VENCIDA, "300.00", "15.00")), tramos);
        var finalCorte = new CorteCartera(FECHA.plusDays(1), List.of(
                operacion(10, 2, EstadoCartera.VENCIDA, "80.00", "4.00"),
                operacion(30, 1, EstadoCartera.POR_VENCER, "250.00", "2.50"),
                operacion(40, 1, EstadoCartera.POR_VENCER, "9999.00", "99.99")), tramos);

        var resultado = servicio.calcularRollRate(inicial, finalCorte);

        assertThat(resultado.cantidadOperacionesIniciales()).isEqualTo(3);
        assertThat(resultado.saldoInicial()).isEqualByComparingTo("600.00");
        var primera = resultado.filas().getFirst();
        assertThat(primera.transiciones().get(1).saldoInicial()).isEqualByComparingTo("100.00");
        assertThat(primera.transiciones().get(2).destino()).isEqualTo("CANCELADA");
        assertThat(primera.transiciones().get(2).idTramoFinal()).isNull();
        assertThat(primera.transiciones().get(2).saldoInicial()).isEqualByComparingTo("200.00");
        assertThat(resultado.filas().get(1).transiciones().getFirst().saldoInicial()).isEqualByComparingTo("300.00");
        for (var fila : resultado.filas()) {
            assertThat(fila.transiciones().stream().map(ResultadoRollRate.Transicion::porcentaje)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
            assertThat(fila.transiciones().stream().map(ResultadoRollRate.Transicion::saldoInicial)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(fila.saldoInicial());
            assertThat(fila.transiciones().stream().mapToLong(ResultadoRollRate.Transicion::cantidadOperaciones).sum())
                    .isEqualTo(fila.cantidadOperaciones());
        }
    }

    @Test
    void tresTransicionesIgualesSumanCienTrasElRedondeo() {
        var inicial = new CorteCartera(FECHA, List.of(
                operacion(1, 1, EstadoCartera.POR_VENCER, "1.00", "0.00"),
                operacion(2, 1, EstadoCartera.POR_VENCER, "1.00", "0.00"),
                operacion(3, 1, EstadoCartera.POR_VENCER, "1.00", "0.00")), tramos);
        var finalCorte = new CorteCartera(FECHA.plusDays(1), List.of(
                operacion(1, 1, EstadoCartera.POR_VENCER, "0.50", "0.00"),
                operacion(2, 2, EstadoCartera.VENCIDA, "0.50", "0.00")), tramos);

        var transiciones = servicio.calcularRollRate(inicial, finalCorte).filas().getFirst().transiciones();

        assertThat(transiciones).extracting(ResultadoRollRate.Transicion::porcentaje)
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
    }

    @Test
    void unCorteFinalVacioCancelaTodaLaCarteraInicial() {
        var inicial = new CorteCartera(FECHA, List.of(
                operacion(1, 1, EstadoCartera.POR_VENCER, "20.00", "0.00")), tramos);
        var finalCorte = new CorteCartera(FECHA.plusDays(1), List.of(), tramos);

        var cancelada = servicio.calcularRollRate(inicial, finalCorte).filas().getFirst().transiciones().getLast();

        assertThat(cancelada.porcentaje()).isEqualByComparingTo("100.00");
        assertThat(cancelada.cantidadOperaciones()).isEqualTo(1);
    }

    @Test
    void unaFilaConSaldoCeroNoInventaPorcentajes() {
        var inicial = new CorteCartera(FECHA, List.of(
                operacion(1, 1, EstadoCartera.POR_VENCER, "0.00", "0.00")), tramos);
        var finalCorte = new CorteCartera(FECHA.plusDays(1), List.of(), tramos);

        var fila = servicio.calcularRollRate(inicial, finalCorte).filas().getFirst();

        assertThat(fila.cantidadOperaciones()).isEqualTo(1);
        assertThat(fila.transiciones()).allSatisfy(celda -> assertThat(celda.porcentaje()).isNull());
    }

    @Test
    void reconoceLaOperacionAunqueCambieElIdDelTramoConfigurado() {
        var inicial = new CorteCartera(FECHA, List.of(
                operacion(1, 1, EstadoCartera.POR_VENCER, "100.00", "1.00")), tramos);
        var finalCorte = new CorteCartera(FECHA.plusDays(1), List.of(
                operacion(1, 99, EstadoCartera.VENCIDA, "90.00", "4.50")),
                List.of(tramo(99, 2, EstadoCartera.VENCIDA)));

        var transicion = servicio.calcularRollRate(inicial, finalCorte).filas().getFirst().transiciones().getFirst();

        assertThat(transicion.idTramoFinal()).isEqualTo(99);
        assertThat(transicion.saldoInicial()).isEqualByComparingTo("100.00");
        assertThat(transicion.porcentaje()).isEqualByComparingTo("100.00");
    }

    @Test
    void rechazaFechasInvertidasYOperacionesFinalesDuplicadas() {
        var vacio = new CorteCartera(FECHA, List.of(), tramos);
        assertThatThrownBy(() -> servicio.calcularRollRate(
                new CorteCartera(FECHA.plusDays(1), List.of(), tramos), vacio))
                .isInstanceOf(IllegalArgumentException.class);
        var operacion = operacion(1, 1, EstadoCartera.POR_VENCER, "10.00", "0.00");
        assertThatThrownBy(() -> servicio.calcularRollRate(vacio,
                new CorteCartera(FECHA.plusDays(1), List.of(operacion, operacion), tramos)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
