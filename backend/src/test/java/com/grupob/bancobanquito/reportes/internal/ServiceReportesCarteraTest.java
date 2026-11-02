package com.grupob.bancobanquito.reportes.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.reportes.CorteCartera;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServiceReportesCarteraTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);
    private final ServiceReportesCarteraImpl servicio = new ServiceReportesCarteraImpl(org.mockito.Mockito.mock(
            com.grupob.bancobanquito.cobranza.ServicioFotografiaCartera.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.cierre.ServiceCierreDia.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.prestamos.ConsultaPrestamoService.class));
    private final List<CorteCartera.Tramo> tramos = List.of(
            tramo(10, 1, EstadoCartera.POR_VENCER),
            tramo(20, 2, EstadoCartera.VENCIDA),
            tramo(30, 3, EstadoCartera.EN_LITIGIO),
            tramo(40, 4, EstadoCartera.CASTIGADA));

    @Test
    void excluyeLaCastigadaDeLaCarteraBrutaYDeLaMorosidad() {
        var resultado = servicio.calcularMorosidad(corte(List.of(
                operacion(1, 10, EstadoCartera.POR_VENCER, "7000.00", "70.00"),
                operacion(2, 20, EstadoCartera.VENCIDA, "2000.00", "100.00"),
                operacion(3, 30, EstadoCartera.EN_LITIGIO, "1000.00", "600.00"),
                operacion(4, 40, EstadoCartera.CASTIGADA, "5000.00", "5000.00"))));

        assertThat(resultado.fechaCorte()).isEqualTo(FECHA);
        assertThat(resultado.saldoPorVencer()).isEqualByComparingTo("7000.00");
        assertThat(resultado.saldoVencido()).isEqualByComparingTo("2000.00");
        assertThat(resultado.saldoEnLitigio()).isEqualByComparingTo("1000.00");
        assertThat(resultado.carteraBruta()).isEqualByComparingTo("10000.00");
        assertThat(resultado.saldoCastigado()).isEqualByComparingTo("5000.00");
        assertThat(resultado.indiceMorosidad()).isEqualByComparingTo("30.00");
    }

    @Test
    void unaCarteraSoloCastigadaNoTieneDenominadorDeMorosidad() {
        var resultado = servicio.calcularMorosidad(corte(List.of(
                operacion(1, 40, EstadoCartera.CASTIGADA, "100.00", "100.00"))));

        assertThat(resultado.carteraBruta()).isZero();
        assertThat(resultado.saldoCastigado()).isEqualByComparingTo("100.00");
        assertThat(resultado.indiceMorosidad()).isNull();
    }

    @Test
    void sumaProvisionesRegistradasSinRecalcularlasConLaTasaDelTramo() {
        var resultado = servicio.calcularComposicion(corte(List.of(
                operacion(1, 10, EstadoCartera.POR_VENCER, "100.00", "7.00"),
                operacion(2, 10, EstadoCartera.POR_VENCER, "200.00", "8.00"),
                operacion(3, 40, EstadoCartera.CASTIGADA, "100.00", "100.00"))));

        assertThat(resultado.cantidadOperaciones()).isEqualTo(3);
        assertThat(resultado.saldoCapital()).isEqualByComparingTo("400.00");
        assertThat(resultado.provisionConstituida()).isEqualByComparingTo("115.00");
        var primero = resultado.tramos().getFirst();
        assertThat(primero.tramo()).isEqualTo(tramos.getFirst());
        assertThat(primero.cantidadOperaciones()).isEqualTo(2);
        assertThat(primero.participacion()).isEqualByComparingTo("75.00");
        assertThat(primero.provisionConstituida()).isEqualByComparingTo("15.00");
        assertThat(resultado.tramos().get(1).cantidadOperaciones()).isZero();
        assertThat(resultado.tramos().get(1).participacion()).isZero();
    }

    @Test
    void distribuyeLosCentavosResidualesSinPerderParticipacion() {
        var resultado = servicio.calcularComposicion(corte(List.of(
                operacion(1, 10, EstadoCartera.POR_VENCER, "1.00", "0.00"),
                operacion(2, 20, EstadoCartera.VENCIDA, "1.00", "0.00"),
                operacion(3, 30, EstadoCartera.EN_LITIGIO, "1.00", "0.00"))));

        assertThat(resultado.tramos().get(0).participacion()).isEqualByComparingTo("33.34");
        assertThat(resultado.tramos().get(1).participacion()).isEqualByComparingTo("33.33");
        assertThat(resultado.tramos().stream().map(detalle -> detalle.participacion())
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
    }

    @Test
    void unCorteVacioConservaLaConfiguracionSinInventarPorcentajes() {
        var resultado = servicio.calcularComposicion(corte(List.of()));

        assertThat(resultado.tramos()).hasSize(4);
        assertThat(resultado.cantidadOperaciones()).isZero();
        assertThat(resultado.saldoCapital()).isZero();
        assertThat(resultado.tramos()).allSatisfy(detalle -> assertThat(detalle.participacion()).isNull());
        assertThat(servicio.calcularMorosidad(corte(List.of())).indiceMorosidad()).isNull();
    }

    @Test
    void rechazaOperacionesDuplicadasAntesDeSumar() {
        var operacion = operacion(1, 10, EstadoCartera.POR_VENCER, "10.00", "0.00");
        assertThatThrownBy(() -> servicio.calcularMorosidad(corte(List.of(operacion, operacion))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicado");
    }

    @Test
    void rechazaTramosDesconocidosYEstadosIncompatibles() {
        assertThatThrownBy(() -> servicio.calcularComposicion(corte(List.of(
                operacion(1, 999, EstadoCartera.POR_VENCER, "10.00", "0.00")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.calcularMorosidad(corte(List.of(
                operacion(1, 10, EstadoCartera.CANCELADA, "10.00", "0.00")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaImportesNegativosYFraccionesDeCentavo() {
        for (String monto : List.of("-0.01", "1.001")) {
            assertThatThrownBy(() -> servicio.calcularMorosidad(corte(List.of(
                    operacion(1, 10, EstadoCartera.POR_VENCER, monto, "0.00")))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> servicio.calcularComposicion(corte(List.of(
                operacion(1, 10, EstadoCartera.POR_VENCER, "1.00", "-0.01")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaTramosRepetidosYConfiguracionesInvalidas() {
        assertThatThrownBy(() -> servicio.calcularComposicion(new CorteCartera(FECHA, List.of(),
                List.of(tramos.getFirst(), tramos.getFirst()))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicados");
        var invalido = new CorteCartera.Tramo(1, 1, 30, 1, EstadoCartera.VENCIDA, new BigDecimal("1.01"));
        assertThatThrownBy(() -> servicio.calcularComposicion(new CorteCartera(FECHA, List.of(), List.of(invalido))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elCorteNoCambiaSiElLlamadorModificaSuLista() {
        List<CorteCartera.Operacion> operaciones = new ArrayList<>();
        CorteCartera corte = corte(operaciones);
        operaciones.add(operacion(1, 10, EstadoCartera.POR_VENCER, "100.00", "1.00"));

        assertThat(servicio.calcularComposicion(corte).cantidadOperaciones()).isZero();
        assertThatThrownBy(() -> corte.operaciones().add(operaciones.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rechazaUnCorteSinFecha() {
        assertThatThrownBy(() -> new CorteCartera(null, List.of(), tramos))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.calcularMorosidad(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CorteCartera corte(List<CorteCartera.Operacion> operaciones) {
        return new CorteCartera(FECHA, operaciones, tramos);
    }

    static CorteCartera.Tramo tramo(int id, int numero, EstadoCartera estado) {
        return new CorteCartera.Tramo(id, numero, numero - 1, numero - 1, estado, new BigDecimal("0.01"));
    }

    static CorteCartera.Operacion operacion(int id, int tramo, EstadoCartera estado, String saldo, String provision) {
        return new CorteCartera.Operacion(id, tramo, estado, new BigDecimal(saldo), new BigDecimal(provision));
    }
}
