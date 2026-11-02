package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.contabilidad.DatosProvision;
import com.grupob.bancobanquito.contabilidad.ResultadoProvisiones;
import com.grupob.bancobanquito.contabilidad.ServiceProvisiones;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProvisionesCarteraTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 15);

    @Test
    @SuppressWarnings("unchecked")
    void enviaSaldoPorcentajeDelTramoYProvisionDeLaFotoAnterior() {
        OperacionCobranzaRepository operaciones = mock(OperacionCobranzaRepository.class);
        TramoCarteraRepository tramos = mock(TramoCarteraRepository.class);
        FotoCarteraRepository fotos = mock(FotoCarteraRepository.class);
        ServiceProvisiones provisiones = mock(ServiceProvisiones.class);
        OperacionCobranza conMora = operacion(1, "VENCIDA", "10000.00", 15);
        OperacionCobranza nueva = operacion(2, "POR_VENCER", "5000.00", 0);
        when(operaciones.findOperacionesActivas()).thenReturn(List.of(conMora, nueva));
        when(tramos.findTramoPorDiasMora(15, FECHA)).thenReturn(Optional.of(tramo("0.0500")));
        when(tramos.findTramoPorDiasMora(0, FECHA)).thenReturn(Optional.of(tramo("0.0100")));
        FotoCartera anterior = new FotoCartera();
        anterior.setValorProvision(new BigDecimal("100.00"));
        when(fotos.findFirstByIdOperacionAndFechaFotoBeforeOrderByFechaFotoDesc(1, FECHA)).thenReturn(Optional.of(anterior));
        when(fotos.findFirstByIdOperacionAndFechaFotoBeforeOrderByFechaFotoDesc(2, FECHA)).thenReturn(Optional.empty());
        when(provisiones.registrarProvisiones(eq(FECHA), anyList())).thenReturn(new ResultadoProvisiones(
                FECHA, 2, 2, new BigDecimal("550.00"), new BigDecimal("450.00")));

        int procesadas = new ServiceProvisionesCarteraImpl(operaciones, tramos, fotos, provisiones).provisionarCartera(FECHA);

        ArgumentCaptor<List<DatosProvision>> datos = ArgumentCaptor.forClass(List.class);
        verify(provisiones).registrarProvisiones(eq(FECHA), datos.capture());
        assertThat(procesadas).isEqualTo(2);
        assertThat(datos.getValue()).containsExactly(
                new DatosProvision(1, new BigDecimal("10000.00"), new BigDecimal("0.0500"), new BigDecimal("100.00"), "VENCIDA"),
                new DatosProvision(2, new BigDecimal("5000.00"), new BigDecimal("0.0100"), new BigDecimal("0.00"), "POR_VENCER"));
    }

    private OperacionCobranza operacion(int id, String estado, String saldo, int diasMora) {
        OperacionCobranza operacion = new OperacionCobranza();
        operacion.setIdOperacion(id);
        operacion.setEstadoCartera(estado);
        operacion.setSaldoCapital(new BigDecimal(saldo));
        operacion.setDiasMora(diasMora);
        return operacion;
    }

    private TramoCartera tramo(String porcentaje) {
        TramoCartera tramo = new TramoCartera();
        tramo.setPorcentajeProvision(new BigDecimal(porcentaje));
        return tramo;
    }
}
