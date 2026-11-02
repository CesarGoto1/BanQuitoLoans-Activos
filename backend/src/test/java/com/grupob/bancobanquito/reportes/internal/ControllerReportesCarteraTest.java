package com.grupob.bancobanquito.reportes.internal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.grupob.bancobanquito.reportes.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ControllerReportesCarteraTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);
    @Mock private ServiceReportesCartera servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControllerReportesCartera(servicio))
                .setControllerAdvice(new ManejadorExcepcionReportes()).build();
    }

    @Test
    void consultaMorosidad() throws Exception {
        when(servicio.consultarMorosidad(FECHA)).thenReturn(new ResultadoMorosidad(FECHA,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO));
        mvc.perform(get("/api/analista/reportes/morosidad").param("fechaCorte", FECHA.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.carteraBruta").value(10));
    }

    @ParameterizedTest
    @ValueSource(strings = {"composicion", "provisiones"})
    void consultaProvisionesRegistradas(String recurso) throws Exception {
        when(servicio.consultarComposicion(FECHA)).thenReturn(new ResultadoComposicionCartera(FECHA,
                List.of(), 0, BigDecimal.ZERO, new BigDecimal("23.45")));
        mvc.perform(get("/api/analista/reportes/" + recurso).param("fechaCorte", FECHA.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.provisionConstituida").value(23.45));
    }

    @Test
    void consultaSalud() throws Exception {
        when(servicio.consultarSalud(FECHA)).thenReturn(new ResultadoSaludCartera(null, null,
                BigDecimal.ZERO, null, List.of(), List.of(), List.of()));
        mvc.perform(get("/api/analista/reportes/salud").param("fechaCorte", FECHA.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.porPlazo").isArray());
    }

    @Test
    void consultaRollRate() throws Exception {
        when(servicio.consultarRollRate(FECHA, FECHA.plusDays(1))).thenReturn(new ResultadoRollRate(
                FECHA, FECHA.plusDays(1), List.of(), 0, BigDecimal.ZERO));
        mvc.perform(get("/api/analista/reportes/roll-rate").param("fechaInicial", FECHA.toString()).param("fechaFinal", "2026-11-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fechaFinal").value("2026-11-01"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "incorrecta"})
    void rechazaFechaInvalida(String fecha) throws Exception {
        mvc.perform(get("/api/analista/reportes/morosidad").param("fechaCorte", fecha)).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    void exigeFecha() throws Exception {
        mvc.perform(get("/api/analista/reportes/morosidad")).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    void impideCorteNoConfirmado() throws Exception {
        when(servicio.consultarRollRate(FECHA, FECHA.plusDays(1))).thenThrow(new IllegalStateException("Fotografía no confirmada"));
        mvc.perform(get("/api/analista/reportes/roll-rate").param("fechaInicial", FECHA.toString()).param("fechaFinal", "2026-11-01"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Fotografía no confirmada"));
    }

    @Test
    void informaDatosInconsistentes() throws Exception {
        when(servicio.consultarSalud(FECHA)).thenThrow(new IllegalArgumentException("Faltan datos originales"));
        mvc.perform(get("/api/analista/reportes/salud").param("fechaCorte", FECHA.toString()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ocultaDetallesInternosDeErroresInesperados() throws Exception {
        when(servicio.consultarMorosidad(FECHA)).thenThrow(new RuntimeException("detalle interno"));
        mvc.perform(get("/api/analista/reportes/morosidad").param("fechaCorte", FECHA.toString()))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.detail").value(
                        "No se pudo consultar el reporte. Verifica la disponibilidad de los datos e intenta nuevamente."));
    }
}
