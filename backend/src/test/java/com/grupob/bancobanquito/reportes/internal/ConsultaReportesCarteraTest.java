package com.grupob.bancobanquito.reportes.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.cobranza.ResumenFotoCartera;
import com.grupob.bancobanquito.cobranza.ResumenTramoCartera;
import com.grupob.bancobanquito.cobranza.ServiceFotografiaCartera;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultaReportesCarteraTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);
    @Mock
    private ServiceFotografiaCartera fotografias;
    @Mock
    private com.grupob.bancobanquito.cierre.ServiceCierreDia cierres;
    @Mock
    private com.grupob.bancobanquito.prestamos.ConsultaPrestamoService prestamos;
    @InjectMocks
    private ServiceReportesCarteraImpl servicio;

    @Test
    void consultaMorosidadConFotografiasYTramosPublicos() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7"),
                foto(2, FECHA, 2, "VENCIDA", "300", "60"),
                foto(3, FECHA, 3, "CASTIGADA", "500", "500")));
        var resultado = servicio.consultarMorosidad(FECHA);
        assertThat(resultado.carteraBruta()).isEqualByComparingTo("1000");
        assertThat(resultado.indiceMorosidad()).isEqualByComparingTo("30");
        assertThat(resultado.saldoCastigado()).isEqualByComparingTo("500");
        verify(fotografias).consultarFotosPorFecha(FECHA);
        verify(fotografias).consultarTramosVigentes(FECHA);
        verifyNoMoreInteractions(fotografias);
    }

    @Test
    void conservaProvisionConstituidaSinRecalcularlaConElPorcentajeDelTramo() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "23")));
        var resultado = servicio.consultarComposicion(FECHA);
        assertThat(resultado.provisionConstituida()).isEqualByComparingTo("23");
        assertThat(resultado.cantidadOperaciones()).isEqualTo(1);
        assertThat(resultado.tramos()).hasSize(3);
        assertThat(resultado.tramos().getFirst().participacion()).isEqualByComparingTo("100");
    }

    @Test
    void coberturaExcluyeProvisionesCastigadas() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "10"),
                foto(2, FECHA, 2, "VENCIDA", "300", "50"),
                foto(3, FECHA, 3, "CASTIGADA", "500", "500")));
        assertThat(servicio.consultarCobertura(FECHA)).isEqualByComparingTo("20");
    }

    @Test
    void carteraVaciaConConfiguracionConservaIndicadoresNoCalculables() {
        preparar(FECHA, List.of());
        assertThat(servicio.consultarMorosidad(FECHA).indiceMorosidad()).isNull();
        assertThat(servicio.consultarCobertura(FECHA)).isNull();
    }

    @Test
    void rollRateUsaSaldoInicialYClasificaAusentesComoCanceladas() {
        LocalDate fin = FECHA.plusDays(1);
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7"),
                foto(2, FECHA, 1, "POR_VENCER", "300", "3")));
        preparar(fin, List.of(foto(1, fin, 2, "VENCIDA", "600", "60"),
                foto(3, fin, 1, "POR_VENCER", "900", "9")));
        confirmar(FECHA, 2);
        confirmar(fin, 2);
        var resultado = servicio.consultarRollRate(FECHA, fin);
        assertThat(resultado.saldoInicial()).isEqualByComparingTo("1000");
        assertThat(resultado.cantidadOperacionesIniciales()).isEqualTo(2);
        var transiciones = resultado.filas().getFirst().transiciones();
        var cancelada = transiciones.stream().filter(t -> t.destino().equals("CANCELADA")).findFirst().orElseThrow();
        assertThat(cancelada.saldoInicial()).isEqualByComparingTo("300");
        assertThat(cancelada.porcentaje()).isEqualByComparingTo("30");
        var vencida = transiciones.stream().filter(t -> Integer.valueOf(2).equals(t.idTramoFinal())).findFirst().orElseThrow();
        assertThat(vencida.saldoInicial()).isEqualByComparingTo("700");
        assertThat(vencida.porcentaje()).isEqualByComparingTo("70");
    }

    @Test
    void rechazaFechasInvalidasAntesDeConsultar() {
        assertThatThrownBy(() -> servicio.consultarMorosidad(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(fotografias);
    }

    @Test
    void rechazaFotografiasDeOtraFecha() {
        preparar(FECHA, List.of(foto(1, FECHA.minusDays(1), 1, "POR_VENCER", "700", "7")));
        assertThatThrownBy(() -> servicio.consultarMorosidad(FECHA)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otra fecha");
    }

    @Test
    void rechazaOperacionesDuplicadas() {
        var foto = foto(1, FECHA, 1, "POR_VENCER", "700", "7");
        preparar(FECHA, List.of(foto, foto));
        assertThatThrownBy(() -> servicio.consultarComposicion(FECHA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaAusenciaDeConfiguracion() {
        when(fotografias.consultarFotosPorFecha(FECHA)).thenReturn(List.of());
        when(fotografias.consultarTramosVigentes(FECHA)).thenReturn(List.of());
        assertThatThrownBy(() -> servicio.consultarMorosidad(FECHA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void impideRollRateSinFotografiaInicial() {
        preparar(FECHA, List.of());
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Fotografía no confirmada");
        verifyNoInteractions(cierres);
    }

    @Test
    void impideFalsasCancelacionesSinFotografiaFinal() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        confirmar(FECHA, 1);
        preparar(FECHA.plusDays(1), List.of());
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(FECHA.plusDays(1).toString());
    }

    @Test
    void noConfirmaCeroProcesadasAunqueHayaRegistros() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        confirmar(FECHA, 0);
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no acreditan");
    }

    @Test
    void rechazaCantidadProcesadaDistintaDeLaAlmacenada() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        confirmar(FECHA, 2);
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no acreditan");
    }

    @Test
    void rechazaEjecucionFallidaAunqueElCierreFigureCompletado() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        cierre("COMPLETADO", List.of(ejecucion(1, "FALLIDA")));
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaCierreNoCompletado() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        cierre("EN_PROCESO", List.of(ejecucion(1, "EXITOSA")));
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("cierre no está completado");
    }

    @Test
    void ultimoIntentoFallidoInvalidaUnExitoAnterior() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        cierre("COMPLETADO", List.of(ejecucion(2, "FALLIDA"), ejecucion(1, "EXITOSA")));
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ausenciaDeEjecucionNoConfirmaFotografiasExistentes() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        cierre("COMPLETADO", List.of());
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("último intento");
    }

    @Test
    void cierreNoConsultableImpideCalculoYConservaLaCausa() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "700", "7")));
        var causa = new IllegalStateException("Cierre no disponible");
        when(cierres.consultarCierre(FECHA)).thenThrow(causa);
        assertThatThrownBy(() -> servicio.consultarRollRate(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasCause(causa);
    }

    private com.grupob.bancobanquito.cierre.EjecucionEtapaCierre ejecucion(int intento, String estado) {
        var inicio = FECHA.atStartOfDay();
        return new com.grupob.bancobanquito.cierre.EjecucionEtapaCierre(6,
                com.grupob.bancobanquito.cierre.PasoCierre.FOTOGRAFIA, intento, estado,
                inicio, inicio.plusMinutes(1), 60000L, 1, null);
    }

    private void cierre(String estado, List<com.grupob.bancobanquito.cierre.EjecucionEtapaCierre> ejecuciones) {
        when(cierres.consultarCierre(FECHA)).thenReturn(new com.grupob.bancobanquito.cierre.ResultadoCierreDia(
                FECHA, estado, ejecuciones));
    }

    @Test
    void saludRelacionaPorIdentificadorYAgrupaAlMismoCliente() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "100", "10"),
                foto(2, FECHA, 2, "VENCIDA", "300", "30"),
                foto(3, FECHA, 3, "CASTIGADA", "900", "900")));
        when(prestamos.buscarDatosPorIds(List.of(1, 2, 3))).thenReturn(List.of(
                dato(3, 99, "30000", 60), dato(2, 10, "10000.01", 37), dato(1, 10, "2000.01", 13)));
        var resultado = servicio.consultarSalud(FECHA);
        assertThat(resultado.concentracion().cantidadClientes()).isEqualTo(1);
        assertThat(resultado.concentracion().mayoresClientes().getFirst().saldoCapital()).isEqualByComparingTo("400");
        assertThat(resultado.concentracion().mayoresClientes().getFirst().cantidadOperaciones()).isEqualTo(2);
        assertThat(resultado.coberturaProvisiones()).isEqualByComparingTo("13.33");
        assertThat(resultado.porMontoOriginal().get(1).saldoCapital()).isEqualByComparingTo("100");
        assertThat(resultado.porMontoOriginal().get(2).participacion()).isEqualByComparingTo("75");
        assertThat(resultado.porPlazo().get(1).cantidadOperaciones()).isEqualTo(1);
        assertThat(resultado.porPlazo().get(2).participacion()).isEqualByComparingTo("75");
        assertThat(resultado.porSistemaAmortizacion().getFirst().participacion()).isEqualByComparingTo("25");
        verify(prestamos).buscarDatosPorIds(List.of(1, 2, 3));
        verifyNoMoreInteractions(prestamos);
    }

    @Test
    void saludVaciaNoConsultaPrestamosYConservaPorcentajesNull() {
        preparar(FECHA, List.of());
        var resultado = servicio.consultarSalud(FECHA);
        assertThat(resultado.coberturaProvisiones()).isNull();
        assertThat(resultado.concentracion().concentracionDiezMayores()).isNull();
        assertThat(resultado.porMontoOriginal()).allSatisfy(c -> assertThat(c.participacion()).isNull());
        verifyNoInteractions(prestamos);
    }

    @Test
    void saludRechazaFaltantesDuplicadosYAjenosALaFotografia() {
        preparar(FECHA, List.of(foto(1, FECHA, 1, "POR_VENCER", "100", "10")));
        var dato = dato(1, 10, "2000", 12);
        when(prestamos.buscarDatosPorIds(List.of(1))).thenReturn(List.of(), List.of(dato, dato),
                List.of(dato(2, 10, "2000", 12)), List.of(dato(1, null, "2000", 12)));
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> servicio.consultarSalud(FECHA)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private com.grupob.bancobanquito.prestamos.DatosOperacionCartera dato(int id, Integer cliente, String monto, int plazo) {
        return new com.grupob.bancobanquito.prestamos.DatosOperacionCartera(id, cliente, new BigDecimal(monto), plazo,
                id == 2 ? com.grupob.bancobanquito.amortizacion.MetodoAmortizacion.ALEMAN
                        : com.grupob.bancobanquito.amortizacion.MetodoAmortizacion.FRANCES);
    }

    private void confirmar(LocalDate fecha, int cantidad) {
        var inicio = fecha.atStartOfDay();
        var ejecucion = new com.grupob.bancobanquito.cierre.EjecucionEtapaCierre(6,
                com.grupob.bancobanquito.cierre.PasoCierre.FOTOGRAFIA, 1, "EXITOSA",
                inicio, inicio.plusMinutes(1), 60000L, cantidad, null);
        when(cierres.consultarCierre(fecha)).thenReturn(new com.grupob.bancobanquito.cierre.ResultadoCierreDia(
                fecha, "COMPLETADO", List.of(ejecucion)));
    }

    private void preparar(LocalDate fecha, List<ResumenFotoCartera> fotos) {
        when(fotografias.consultarFotosPorFecha(fecha)).thenReturn(fotos);
        when(fotografias.consultarTramosVigentes(fecha)).thenReturn(List.of(
                tramo(1, "POR_VENCER"), tramo(2, "VENCIDA"), tramo(3, "CASTIGADA")));
    }

    private ResumenTramoCartera tramo(int id, String estado) {
        return new ResumenTramoCartera(id, id, 0, null, estado, true, "NORMAL",
                new BigDecimal("0.01"), FECHA.minusYears(1), null);
    }

    private ResumenFotoCartera foto(int id, LocalDate fecha, int tramo, String estado, String saldo, String provision) {
        return new ResumenFotoCartera(id, fecha, tramo, estado, 0, new BigDecimal(saldo),
                new BigDecimal(provision), BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
