package com.grupob.bancobanquito.cierre.internal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupob.bancobanquito.cierre.EjecucionEtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.cierre.ResultadoCierreDia;
import com.grupob.bancobanquito.cierre.ServiceCierreDia;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ControllerCierreDiaTest {

    private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 10, 31);
    private static final String ANALISTA = "1712345675";

    @Mock
    private ServiceCierreDia closingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerCierreDia(closingService))
                .setControllerAdvice(new ManejadorExcepcionCierre())
                .build();
    }

    @Test
    void ejecutaElCierreConLaCedulaDelAnalistaYDevuelveLaBitacora() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 31, 23, 0);
        when(closingService.ejecutarCierre(FECHA_NEGOCIO, ANALISTA)).thenReturn(new ResultadoCierreDia(
                FECHA_NEGOCIO, "COMPLETADO", List.of(new EjecucionEtapaCierre(1, PasoCierre.RECAUDACIONES, 1, "EXITOSA",
                        inicio, inicio.plusSeconds(2), 2000L, 3, null))));

        mockMvc.perform(post("/api/analista/cierres-dia")
                        .header("X-Identificacion", ANALISTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaNegocio\":\"2026-10-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCierre").value("COMPLETADO"))
                .andExpect(jsonPath("$.ejecuciones[0].etapa").value("RECAUDACIONES"))
                .andExpect(jsonPath("$.ejecuciones[0].duracionMilisegundos").value(2000))
                .andExpect(jsonPath("$.ejecuciones[0].registrosProcesados").value(3));
    }

    @Test
    void sinFechaDeNegocioResponde400() throws Exception {
        mockMvc.perform(post("/api/analista/cierres-dia")
                        .header("X-Identificacion", ANALISTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica la fecha de negocio que se va a cerrar."));
        verifyNoInteractions(closingService);
    }

    @Test
    void unDiaYaCerradoResponde409ConElMotivo() throws Exception {
        when(closingService.ejecutarCierre(FECHA_NEGOCIO, ANALISTA))
                .thenThrow(new ExcepcionCierre(HttpStatus.CONFLICT, "El día 2026-10-31 ya fue cerrado."));

        mockMvc.perform(post("/api/analista/cierres-dia")
                        .header("X-Identificacion", ANALISTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaNegocio\":\"2026-10-31\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("No se pudo procesar el cierre"))
                .andExpect(jsonPath("$.detail").value("El día 2026-10-31 ya fue cerrado."));
    }

    @Test
    void consultaElCierreDeUnDia() throws Exception {
        when(closingService.consultarCierre(FECHA_NEGOCIO))
                .thenReturn(new ResultadoCierreDia(FECHA_NEGOCIO, "FALLIDO", List.of()));

        mockMvc.perform(get("/api/analista/cierres-dia/2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaNegocio").value("2026-10-31"))
                .andExpect(jsonPath("$.estadoCierre").value("FALLIDO"));
    }

    @Test
    void unaFechaMalEscritaResponde400() throws Exception {
        mockMvc.perform(get("/api/analista/cierres-dia/31-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parámetro fechaNegocio tiene un valor inválido."));
    }
}
