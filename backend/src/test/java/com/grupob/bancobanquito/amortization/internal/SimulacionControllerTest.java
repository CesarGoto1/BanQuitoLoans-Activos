package com.grupob.bancobanquito.amortization.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SimulacionControllerTest {
    private SimulacionService servicio;
    private MockMvc mvc;
    private static final String RUTA = "/api/cliente/simulaciones";
    private static final String VALIDA = "{\"montoSolicitado\":10000.00,\"plazoMeses\":12,\"sistemaAmortizacion\":\"FRANCES\"}";

    @BeforeEach
    void preparar() {
        servicio = mock(SimulacionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SimulacionController(servicio))
                .setControllerAdvice(new SimulacionExceptionHandler()).build();
    }

    @Test
    void responder200SinCrearRecurso() throws Exception {
        mvc.perform(post(RUTA).header("X-Identificacion", "1700000000").header("X-Rol", "CLIENTE")
                .contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        verify(servicio).simular(any());
    }

    @Test
    void rechazarCamposInvalidosSinInvocarServicio() throws Exception {
        mvc.perform(post(RUTA).header("X-Identificacion", "1700000000").header("X-Rol", "CLIENTE")
                .contentType(MediaType.APPLICATION_JSON).content("{\"montoSolicitado\":-1,\"plazoMeses\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazarMasDeDosDecimales() throws Exception {
        mvc.perform(post(RUTA).header("X-Identificacion", "1700000000").header("X-Rol", "CLIENTE")
                .contentType(MediaType.APPLICATION_JSON).content(VALIDA.replace("10000.00", "10000.001")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazarSistemaDesconocido() throws Exception {
        mvc.perform(post(RUTA).header("X-Identificacion", "1700000000").header("X-Rol", "CLIENTE")
                .contentType(MediaType.APPLICATION_JSON).content(VALIDA.replace("FRANCES", "OTRO")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Solicitud inválida"));
    }

    @Test
    void requerirIdentificacionYRol() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void impedirRolAnalista() throws Exception {
        mvc.perform(post(RUTA).header("X-Identificacion", "1700000000").header("X-Rol", "ANALISTA")
                .contentType(MediaType.APPLICATION_JSON).content(VALIDA)).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void exponerConflictoComoProblemDetail() throws Exception {
        when(servicio.simular(any())).thenThrow(new CondicionesSimulacionException("Monto fuera del rango vigente."));
        mvc.perform(post(RUTA).header("X-Identificacion", "1700000000").header("X-Rol", "CLIENTE")
                .contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Monto fuera del rango vigente."));
    }

    @Test
    void ocultarDetallesInternosDeConfiguracion() throws Exception {
        when(servicio.consultarParametros()).thenThrow(new IllegalStateException("secreto interno"));
        mvc.perform(get(RUTA + "/parametros").header("X-Identificacion", "1700000000").header("X-Rol", "CLIENTE"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("No se pudieron consultar las condiciones vigentes. Intenta nuevamente o contacta al administrador."));
    }
}
