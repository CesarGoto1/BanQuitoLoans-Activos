package com.grupob.bancobanquito.cobranza.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.cobranza.ArchivoRecaudacionSolicitado;
import com.grupob.bancobanquito.pasivos.ArchivoRecaudacionGenerado;
import com.grupob.bancobanquito.pasivos.PasivosNoDisponibleException;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ServiceSolicitudArchivoRecaudacionTest {

    private final ServicePasivos servicioPasivos = mock(ServicePasivos.class);
    private final ServiceFechaNegocio servicioFechaNegocio = mock(ServiceFechaNegocio.class);
    private final ServiceSolicitudArchivoRecaudacionImpl servicio =
            new ServiceSolicitudArchivoRecaudacionImpl(servicioPasivos, servicioFechaNegocio);

    @Test
    void pideElArchivoDeLaFechaDeNegocioVigenteYNoDeLaFechaDelReloj() {
        LocalDate fechaNegocio = LocalDate.of(2026, 11, 2);
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(fechaNegocio);
        when(servicioPasivos.generarArchivoRecaudacion(fechaNegocio))
                .thenReturn(new ArchivoRecaudacionGenerado("REC_20261102_001.csv", 2, new BigDecimal("150.00")));

        ArchivoRecaudacionSolicitado solicitado = servicio.solicitarArchivoDeLaFechaVigente();

        assertThat(solicitado.fechaProceso()).isEqualTo(fechaNegocio);
        assertThat(solicitado.nombreArchivo()).isEqualTo("REC_20261102_001.csv");
        assertThat(solicitado.cantidadCobros()).isEqualTo(2);
        assertThat(solicitado.montoTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void siPasivosNoRespondeLaExcepcionSube() {
        LocalDate fechaNegocio = LocalDate.of(2026, 11, 2);
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(fechaNegocio);
        when(servicioPasivos.generarArchivoRecaudacion(fechaNegocio))
                .thenThrow(new PasivosNoDisponibleException(new RuntimeException("caído")));

        assertThatThrownBy(servicio::solicitarArchivoDeLaFechaVigente)
                .isInstanceOf(PasivosNoDisponibleException.class);
    }
}
