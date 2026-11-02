package com.grupob.bancobanquito.reportes.internal;

import static com.grupob.bancobanquito.reportes.internal.ServiceReportesCarteraTest.operacion;
import static com.grupob.bancobanquito.reportes.internal.ServiceReportesCarteraTest.tramo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.reportes.CorteCartera;
import com.grupob.bancobanquito.reportes.DatosOperacionReporte;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraConcentracionTest {

    private final ServiceReportesCarteraImpl servicio = new ServiceReportesCarteraImpl(org.mockito.Mockito.mock(
            com.grupob.bancobanquito.cobranza.ServiceFotografiaCartera.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.cierre.ServiceCierreDia.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.prestamos.ConsultaPrestamoService.class));
    private final List<CorteCartera.Tramo> tramos = List.of(
            tramo(1, 1, EstadoCartera.POR_VENCER), tramo(2, 2, EstadoCartera.CASTIGADA));

    @Test
    void agrupaPrestamosDelMismoClienteAntesDeElegirLosDiezMayores() {
        List<CorteCartera.Operacion> operaciones = new ArrayList<>();
        List<DatosOperacionReporte> datos = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            operaciones.add(operacion(i, 1, EstadoCartera.POR_VENCER, "100.00", "1.00"));
            datos.add(dato(i, i));
        }
        operaciones.add(operacion(12, 1, EstadoCartera.POR_VENCER, "100.00", "1.00"));
        datos.add(dato(12, 11));
        operaciones.add(operacion(13, 2, EstadoCartera.CASTIGADA, "100000.00", "100000.00"));
        datos.add(dato(13, 12));

        var resultado = servicio.calcularConcentracion(corte(operaciones), datos);

        assertThat(resultado.carteraBruta()).isEqualByComparingTo("1200.00");
        assertThat(resultado.cantidadClientes()).isEqualTo(11);
        assertThat(resultado.mayoresClientes()).hasSize(10);
        assertThat(resultado.mayoresClientes().getFirst().idCliente()).isEqualTo(11);
        assertThat(resultado.mayoresClientes().getFirst().cantidadOperaciones()).isEqualTo(2);
        assertThat(resultado.mayoresClientes().get(1).idCliente()).isEqualTo(1);
        assertThat(resultado.saldoMayoresClientes()).isEqualByComparingTo("1100.00");
        assertThat(resultado.concentracionDiezMayores()).isEqualByComparingTo("91.67");
    }

    @Test
    void menosDeDiezClientesRepresentanTodaLaCarteraBruta() {
        var resultado = servicio.calcularConcentracion(corte(List.of(
                operacion(1, 1, EstadoCartera.POR_VENCER, "100.00", "1.00"))), List.of(dato(1, 5)));

        assertThat(resultado.concentracionDiezMayores()).isEqualByComparingTo("100.00");
        assertThat(resultado.mayoresClientes()).hasSize(1);
    }

    @Test
    void noCalculaUnPorcentajeCuandoLaCarteraBrutaEsCero() {
        var resultado = servicio.calcularConcentracion(corte(List.of()), List.of());

        assertThat(resultado.concentracionDiezMayores()).isNull();
        assertThat(resultado.mayoresClientes()).isEmpty();
    }

    @Test
    void rechazaDatosDeClientesFaltantesODuplicados() {
        var corte = corte(List.of(operacion(1, 1, EstadoCartera.POR_VENCER, "100.00", "1.00")));

        assertThatThrownBy(() -> servicio.calcularConcentracion(corte, List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Faltan datos");
        assertThatThrownBy(() -> servicio.calcularConcentracion(corte, List.of(dato(1, 1), dato(1, 2))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicados");
    }

    private DatosOperacionReporte dato(int operacion, int cliente) {
        return new DatosOperacionReporte(operacion, cliente, new BigDecimal("100.00"), 12,
                MetodoAmortizacion.FRANCES);
    }

    private CorteCartera corte(List<CorteCartera.Operacion> operaciones) {
        return new CorteCartera(LocalDate.of(2026, 10, 31), operaciones, tramos);
    }
}
