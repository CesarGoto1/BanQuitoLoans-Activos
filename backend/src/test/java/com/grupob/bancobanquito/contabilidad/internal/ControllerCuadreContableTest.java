package com.grupob.bancobanquito.contabilidad.internal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupob.bancobanquito.contabilidad.ResultadoCuadreContable;
import com.grupob.bancobanquito.contabilidad.ServiceCuadreContable;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ControllerCuadreContableTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);
    private static final String RECURSO = "/api/analista/cuadres-contables";

    @Mock
    ServiceCuadreContable servicioCuadre;

    MockMvc mockMvc;

    @BeforeEach
    void prepararControlador() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerCuadreContable(servicioCuadre))
                .setControllerAdvice(new ManejadorExcepcionCuadre()).build();
    }

    @Test
    void devuelveLosTotalesYElAlcanceDelCuadre() throws Exception {
        when(servicioCuadre.consultarCuadre(FECHA)).thenReturn(new ResultadoCuadreContable(
                FECHA, new BigDecimal("10000.00"), new BigDecimal("10000.00"), new BigDecimal("0.00"),
                2, true, "La igualdad no verifica todos los eventos de negocio."));

        mockMvc.perform(get(RECURSO).param("fechaNegocio", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaNegocio").value("2026-10-31"))
                .andExpect(jsonPath("$.totalDebitos").value(10000))
                .andExpect(jsonPath("$.totalCreditos").value(10000))
                .andExpect(jsonPath("$.diferencia").value(0))
                .andExpect(jsonPath("$.cantidadAsientos").value(2))
                .andExpect(jsonPath("$.cuadrado").value(true))
                .andExpect(jsonPath("$.observacion").value("La igualdad no verifica todos los eventos de negocio."));
    }

    @Test
    void devuelveUnaFechaSinAsientosComoResultadoVacioExplicito() throws Exception {
        when(servicioCuadre.consultarCuadre(FECHA)).thenReturn(new ResultadoCuadreContable(
                FECHA, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, true,
                "No hay asientos registrados para la fecha consultada."));

        mockMvc.perform(get(RECURSO).param("fechaNegocio", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadAsientos").value(0))
                .andExpect(jsonPath("$.observacion").value("No hay asientos registrados para la fecha consultada."));
    }

    @Test
    void exigeLaFechaDeNegocio() throws Exception {
        mockMvc.perform(get(RECURSO))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Indica una fecha de negocio válida con formato AAAA-MM-DD."));
        verifyNoInteractions(servicioCuadre);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "31-10-2026", "2026-02-30", "2026-13-01", "texto"})
    void rechazaFechasInvalidasSinConsultar(String fecha) throws Exception {
        mockMvc.perform(get(RECURSO).param("fechaNegocio", fecha))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(servicioCuadre);
    }

    @Test
    void noExponeDetallesTecnicosDeUnFallo() throws Exception {
        when(servicioCuadre.consultarCuadre(FECHA)).thenThrow(new IllegalStateException("detalle interno SQL"));

        mockMvc.perform(get(RECURSO).param("fechaNegocio", "2026-10-31"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error al consultar la contabilidad. Intenta nuevamente."));
    }
}
