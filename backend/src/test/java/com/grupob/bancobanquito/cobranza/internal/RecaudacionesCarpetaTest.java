package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.pagos.ServicePagos;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecaudacionesCarpetaTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);

    @TempDir
    Path raiz;

    private LectorArchivoRecaudacion lector;
    private ServiceRecaudacionesImpl servicio;
    private Path entrada;
    private Path novedades;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        entrada = raiz.resolve("entrada");
        novedades = raiz.resolve("novedades");
        lector = mock(LectorArchivoRecaudacion.class);
        ArchivoRecaudacionRepository archivos = mock(ArchivoRecaudacionRepository.class);
        when(archivos.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        ServiceFechaNegocio fechaNegocio = mock(ServiceFechaNegocio.class);
        when(fechaNegocio.getCurrentBusinessDate()).thenReturn(FECHA);
        servicio = new ServiceRecaudacionesImpl(lector, archivos, mock(RegistroRecaudacionRepository.class),
                fechaNegocio, mock(ObjectProvider.class), mock(PlatformTransactionManager.class),
                new EscritorNovedadesRecaudacion(), entrada.toString(), novedades.toString());
    }

    @Test
    void diaSinCarpetaNiArchivosCierraSinProcesar() throws IOException {
        assertThat(servicio.procesarCarpeta(FECHA)).isZero();
        Files.createDirectories(entrada);
        Files.writeString(entrada.resolve("REC_20261030_001.csv"), "x");
        assertThat(servicio.procesarCarpeta(FECHA)).isZero();
    }

    @Test
    void procesaSoloArchivosDelDiaYNoLosRepiteEnUnReintento() throws IOException {
        Files.createDirectories(entrada);
        Files.writeString(entrada.resolve("REC_20261031_001.csv"), "invalido");
        Files.writeString(entrada.resolve("REC_20261030_001.csv"), "otro dia");
        when(lector.leer(eq("REC_20261031_001.csv"), any(byte[].class), eq(FECHA)))
                .thenThrow(new ArchivoRecaudacionInvalidoException("ARC-002", "Estructura invalida."));

        assertThat(servicio.procesarCarpeta(FECHA)).isEqualTo(1);
        assertThat(servicio.procesarCarpeta(FECHA)).isZero();

        assertThat(Files.readString(novedades.resolve("NOV_REC_20261031_001.csv")))
                .isEqualTo("H,REC_20261031_001,2026-10-31,RECHAZADO,ARC-002\n");
        assertThat(novedades.resolve("NOV_REC_20261030_001.csv")).doesNotExist();
        verify(lector, times(1)).leer(eq("REC_20261031_001.csv"), any(byte[].class), eq(FECHA));
    }
}
