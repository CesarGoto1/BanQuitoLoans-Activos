package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.LoanDecisionResult;
import com.grupob.bancobanquito.origination.LoanDecisionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoanDecisionControllerTest {

    private final LoanDecisionService service = mock(LoanDecisionService.class);
    private MockMvc mvc;
    private static final String APPROVAL = "/api/analista/solicitudes/7/aprobacion";
    private static final String REJECTION = "/api/analista/solicitudes/7/rechazo";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LoanDecisionController(service))
                .setControllerAdvice(new SolicitudExceptionHandler()).build();
    }

    @Test
    void approvesUsingTheEstablishedJsonContract() throws Exception {
        when(service.approveApplication(7, "1710000001", new BigDecimal("0.1650")))
                .thenReturn(new LoanDecisionResult(7, "APROBADA", new BigDecimal("0.1650"),
                        new BigDecimal("910.20"), LocalDate.of(2026, 10, 31), null, 42));
        mvc.perform(post(APPROVAL).header("X-Rol", "ANALISTA").header("X-Identificacion", "1710000001")
                .contentType(MediaType.APPLICATION_JSON).content("{\"teaAsignada\":0.1650}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoSolicitud").value("APROBADA"))
                .andExpect(jsonPath("$.teaAsignada").isNumber())
                .andExpect(jsonPath("$.idAnalista").value(42))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void rejectsWithAReasonAndUsesHeaderIdentity() throws Exception {
        mvc.perform(post(REJECTION).header("X-Rol", "ANALISTA").header("X-Identificacion", "1710000001")
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivoRechazo\":\"Capacidad insuficiente\",\"idAnalista\":999}"))
                .andExpect(status().isOk());
        verify(service).rejectApplication(7, "1710000001", "Capacidad insuficiente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"teaAsignada\":null}", "{\"teaAsignada\":-0.1}",
            "{\"teaAsignada\":0}", "{\"teaAsignada\":0.16501}", "{\"teaAsignada\":\"abc\"}", "{"})
    void returnsBadRequestForInvalidRates(String body) throws Exception {
        mvc.perform(post(APPROVAL).header("X-Rol", "ANALISTA").header("X-Identificacion", "1710000001")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").isNotEmpty()).andExpect(jsonPath("$.detail").isNotEmpty());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void refusesBlankReason(String reason) throws Exception {
        String body = "{\"motivoRechazo\":\"" + reason.replace("\t", "\\t") + "\"}";
        mvc.perform(post(REJECTION).header("X-Rol", "ANALISTA").header("X-Identificacion", "1710000001")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void refusesReasonLongerThanDatabaseColumn() throws Exception {
        mvc.perform(post(REJECTION).header("X-Rol", "ANALISTA").header("X-Identificacion", "1710000001")
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivoRechazo\":\"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void refusesIncorrectRole() throws Exception {
        mvc.perform(post(APPROVAL).header("X-Rol", "CLIENTE").header("X-Identificacion", "1710000001")
                .contentType(MediaType.APPLICATION_JSON).content("{\"teaAsignada\":0.16}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(service);
    }

    @Test
    void requiresIdentification() throws Exception {
        mvc.perform(post(APPROVAL).header("X-Rol", "ANALISTA")
                .contentType(MediaType.APPLICATION_JSON).content("{\"teaAsignada\":0.16}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForUnknownApplication() throws Exception {
        when(service.approveApplication(any(), anyString(), any())).thenThrow(new SolicitudNoEncontradaException(7));
        assertFailure(404);
    }

    @Test
    void returnsConflictForBusinessRule() throws Exception {
        when(service.approveApplication(any(), anyString(), any())).thenThrow(new LoanDecisionRuleException("La solicitud ya fue resuelta."));
        assertFailure(409);
    }

    @Test
    void returnsConflictWhenAnotherTransactionOwnsTheLock() throws Exception {
        when(service.approveApplication(any(), anyString(), any())).thenThrow(new CannotAcquireLockException("internal"));
        assertFailure(409);
    }

    @Test
    void hidesUnexpectedFailureDetails() throws Exception {
        when(service.approveApplication(any(), anyString(), any())).thenThrow(new IllegalStateException("private database details"));
        assertFailure(500);
    }

    private void assertFailure(int expectedStatus) throws Exception {
        mvc.perform(post(APPROVAL).header("X-Rol", "ANALISTA").header("X-Identificacion", "1710000001")
                .contentType(MediaType.APPLICATION_JSON).content("{\"teaAsignada\":0.16}"))
                .andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private database details"))));
    }
}
