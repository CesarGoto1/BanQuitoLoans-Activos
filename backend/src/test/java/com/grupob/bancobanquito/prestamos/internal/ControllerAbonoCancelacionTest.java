package com.grupob.bancobanquito.prestamos.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import com.grupob.bancobanquito.prestamos.CuadroAmortizacion;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.LiquidacionCancelacion;
import com.grupob.bancobanquito.prestamos.ResultadoAbono;
import com.grupob.bancobanquito.prestamos.ServiceAbonoExtraordinario;
import com.grupob.bancobanquito.prestamos.ServiceCancelacionAnticipada;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControllerAbonoCancelacionTest {

    private static final String OPERACION = "/api/analista/operaciones/OP-20261031-000001";
    private static final String ANALISTA = "1700000001";

    private final ServiceAbonoExtraordinario servicioAbono = mock(ServiceAbonoExtraordinario.class);
    private final ServiceCancelacionAnticipada servicioCancelacion = mock(ServiceCancelacionAnticipada.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControllerAbonoCancelacion(servicioAbono, servicioCancelacion))
                .setControllerAdvice(new ManejadorExcepcionAbonoCancelacion()).build();
    }

    @Test
    void simulaElAbono() throws Exception {
        when(servicioAbono.simular("OP-20261031-000001", new BigDecimal("2000.00"), ModalidadAbono.DISMINUIR_CUOTA))
                .thenReturn(resultadoAbono());

        mvc.perform(post(OPERACION + "/simulaciones-abono").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":2000.00,\"modalidad\":\"DISMINUIR_CUOTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interesLiquidado").value(59.98))
                .andExpect(jsonPath("$.despues.valorCuota").value(716.73));
    }

    @Test
    void registraElAbonoConElAnalistaDeLaCabecera() throws Exception {
        when(servicioAbono.registrar("OP-20261031-000001", new BigDecimal("2000.00"),
                ModalidadAbono.DISMINUIR_PLAZO, ANALISTA)).thenReturn(resultadoAbono());

        mvc.perform(post(OPERACION + "/abonos").header("X-Identificacion", ANALISTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":2000.00,\"modalidad\":\"DISMINUIR_PLAZO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoNuevo").value(7284.57));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"monto\":2000.00}", "{\"monto\":-1,\"modalidad\":\"DISMINUIR_CUOTA\"}",
            "{\"monto\":100.001,\"modalidad\":\"DISMINUIR_CUOTA\"}", "{\"monto\":100,\"modalidad\":\"OTRA\"}",
            "no es json"})
    void rechazaPeticionesDeAbonoInvalidas(String cuerpo) throws Exception {
        mvc.perform(post(OPERACION + "/simulaciones-abono").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        verifyNoInteractions(servicioAbono);
    }

    @Test
    void devuelveElErrorDeNegocioComoProblemDetail() throws Exception {
        when(servicioAbono.simular(anyString(), any(), any())).thenThrow(new ExcepcionPrestamo(HttpStatus.CONFLICT,
                "La operación tiene cuotas vencidas; el valor debe aplicarse como pago con prelación, no como abono."));

        mvc.perform(post(OPERACION + "/simulaciones-abono").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":2000.00,\"modalidad\":\"DISMINUIR_CUOTA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("No se pudo procesar"));
    }

    @Test
    void consultaElValorDeCancelacion() throws Exception {
        when(servicioCancelacion.consultar("OP-20261031-000001")).thenReturn(liquidacion());

        mvc.perform(get(OPERACION + "/valor-cancelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorCancelacion").value(9284.57));
    }

    @Test
    void registraLaCancelacion() throws Exception {
        when(servicioCancelacion.registrar("OP-20261031-000001", new BigDecimal("9284.57"), ANALISTA))
                .thenReturn(liquidacion());

        mvc.perform(post(OPERACION + "/cancelaciones").header("X-Identificacion", ANALISTA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montoRecibido\":9284.57}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCartera").value("POR_VENCER"));
    }

    @Test
    void rechazaUnaCancelacionSinValor() throws Exception {
        mvc.perform(post(OPERACION + "/cancelaciones").header("X-Identificacion", ANALISTA)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicioCancelacion);
    }

    private ResultadoAbono resultadoAbono() {
        CuadroAmortizacion antes = new CuadroAmortizacion(List.of(), 11, new BigDecimal("911.44"),
                new BigDecimal("675.22"), LocalDate.of(2027, 10, 31));
        CuadroAmortizacion despues = new CuadroAmortizacion(List.of(), 11, new BigDecimal("716.73"),
                new BigDecimal("533.47"), LocalDate.of(2027, 10, 31));
        return new ResultadoAbono("OP-20261031-000001", LocalDate.of(2026, 12, 15), ModalidadAbono.DISMINUIR_CUOTA,
                new BigDecimal("2000.00"), new BigDecimal("59.98"), new BigDecimal("1940.02"),
                new BigDecimal("9224.59"), new BigDecimal("7284.57"), antes, despues);
    }

    private LiquidacionCancelacion liquidacion() {
        return new LiquidacionCancelacion("OP-20261031-000001", LocalDate.of(2026, 12, 15), EstadoCartera.POR_VENCER,
                new BigDecimal("9224.59"), new BigDecimal("59.98"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("9284.57"));
    }
}
