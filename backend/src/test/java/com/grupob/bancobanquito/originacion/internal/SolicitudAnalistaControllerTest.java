package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.originacion.SolicitudBandejaResponse;
import com.grupob.bancobanquito.originacion.SolicitudDetalleResponse;
import com.grupob.bancobanquito.originacion.ServiceBandejaSolicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SolicitudAnalistaControllerTest {

    @Mock
    private ServiceBandejaSolicitud solicitudService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SolicitudAnalistaController controller = new SolicitudAnalistaController(solicitudService);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new SolicitudExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Debe rechazar con 403 si falta la cabecera X-Rol o su valor no es ANALISTA")
    void debeRechazarCon403SiRolNoEsAnalista() throws Exception {
        mockMvc.perform(get("/api/analista/solicitudes")
                        .header("X-Rol", "CLIENTE")
                        .header("X-Identificacion", "1710000001"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acceso no autorizado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Acceso denegado: se requiere la cabecera X-Rol con valor ANALISTA."));
    }

    @Test
    @DisplayName("Debe rechazar con 400 si falta la cabecera X-Identificacion")
    void debeRechazarCon400SiFaltaIdentificacion() throws Exception {
        mockMvc.perform(get("/api/analista/solicitudes")
                        .header("X-Rol", "ANALISTA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Cabecera de autenticación requerida"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("La cabecera X-Identificacion es requerida para identificar al analista."));
    }

    @Test
    @DisplayName("Debe retornar 200 con la bandeja de solicitudes cuando las cabeceras son válidas")
    void debeRetornarBandejaCon200() throws Exception {
        SolicitudBandejaResponse item = new SolicitudBandejaResponse(
                1, 101, new BigDecimal("5000.00"), 12, "FRANCES",
                "EDUCACION", new BigDecimal("450.00"), LocalDate.of(2026, 10, 24),
                LocalDateTime.of(2026, 10, 24, 10, 0), "REGISTRADA"
        );

        when(solicitudService.listarBandejaAnalista("REGISTRADA")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/analista/solicitudes")
                        .header("X-Rol", "ANALISTA")
                        .header("X-Identificacion", "1710000001")
                        .param("estado", "REGISTRADA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSolicitud").value(1))
                .andExpect(jsonPath("$[0].estadoSolicitud").value("REGISTRADA"))
                .andExpect(jsonPath("$[0].montoSolicitado").value(5000.00));
    }

    @Test
    @DisplayName("Debe retornar 200 con el detalle financiero de la solicitud")
    void debeRetornarDetalleCon200() throws Exception {
        SolicitudDetalleResponse detalle = new SolicitudDetalleResponse(
                1, 101, "REGISTRADA", LocalDate.of(2026, 10, 24),
                LocalDateTime.of(2026, 10, 24, 10, 0),
                new BigDecimal("5000.00"), 12, "FRANCES", "EDUCACION",
                new BigDecimal("300.00"), new BigDecimal("1500.00"), new BigDecimal("500.00"),
                new BigDecimal("1000.00"), new BigDecimal("30.00"), new BigDecimal("40.00"),
                new BigDecimal("400.00"), true, "Capacidad válida",
                null, null, null, null, null
        );

        when(solicitudService.obtenerDetalleParaEvaluacion(1)).thenReturn(detalle);

        mockMvc.perform(get("/api/analista/solicitudes/1")
                        .header("X-Rol", "ANALISTA")
                        .header("X-Identificacion", "1710000001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitud").value(1))
                .andExpect(jsonPath("$.ingresoRemanente").value(1000.00))
                .andExpect(jsonPath("$.porcentajeImpactoCuota").value(30.00))
                .andExpect(jsonPath("$.capacidadPagoValida").value(true));
    }

    @Test
    @DisplayName("Debe retornar 404 ProblemDetail si la solicitud no existe")
    void debeRetornar404SiSolicitudNoExiste() throws Exception {
        when(solicitudService.obtenerDetalleParaEvaluacion(999))
                .thenThrow(new SolicitudNoEncontradaException(999));

        mockMvc.perform(get("/api/analista/solicitudes/999")
                        .header("X-Rol", "ANALISTA")
                        .header("X-Identificacion", "1710000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Solicitud de crédito no encontrada"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No se encontró la solicitud de crédito con ID: 999"));
    }
}
