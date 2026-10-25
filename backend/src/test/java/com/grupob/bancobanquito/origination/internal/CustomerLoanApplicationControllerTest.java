package com.grupob.bancobanquito.origination.internal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupob.bancobanquito.origination.LoanApplicationService;
import com.grupob.bancobanquito.origination.internal.LoanApplication.ApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CustomerLoanApplicationControllerTest {

    private static final String URL = "/api/customer/loan-applications";
    private static final String IDENTIFICATION = "1711000073";
    private static final String VALID_REQUEST = """
            {"requestedAmount": 10000.00, "termMonths": 12, "amortizationSystem": "FRENCH",
             "creditPurpose": "DURABLE_GOODS", "monthlyIncome": 3500.00, "monthlyExpenses": 1200.00}
            """;

    private LoanApplicationService loanApplicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        loanApplicationService = mock(LoanApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerLoanApplicationController(loanApplicationService))
                .setControllerAdvice(new LoanApplicationExceptionHandler())
                .build();
    }

    private ResultActions send(String identification, String body) throws Exception {
        var request = post(URL).contentType(MediaType.APPLICATION_JSON).content(body);
        if (identification != null) {
            request.header("X-Identificacion", identification).header("X-Rol", "CLIENTE");
        }
        return mockMvc.perform(request);
    }

    @Test
    void registersTheApplicationWithTheIdentificationFromTheHeader() throws Exception {
        when(loanApplicationService.register(eq(IDENTIFICATION), any())).thenReturn(new LoanApplicationResponse(15,
                ApplicationStatus.REGISTERED, LocalDate.of(2026, 10, 31), new BigDecimal("0.1550"),
                new BigDecimal("906.21"), new BigDecimal("2300.00"), new BigDecimal("920.00"),
                new BigDecimal("39.40"), null));

        send(IDENTIFICATION, VALID_REQUEST)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.applicationDate").value("2026-10-31"))
                .andExpect(jsonPath("$.maxAllowedInstallment").value(920.00));

        verify(loanApplicationService).register(eq(IDENTIFICATION), any());
    }

    @Test
    void missingFieldRespondsBadRequestWithItsSpanishLabel() throws Exception {
        send(IDENTIFICATION, VALID_REQUEST.replace("\"requestedAmount\": 10000.00, ", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos de la solicitud inválidos"))
                .andExpect(jsonPath("$.detail").value("Revise los campos: monto solicitado"));

        verify(loanApplicationService, never()).register(anyString(), any());
    }

    @Test
    void unknownAmortizationSystemRespondsBadRequest() throws Exception {
        send(IDENTIFICATION, VALID_REQUEST.replace("FRENCH", "ITALIAN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revise los campos: sistema de amortización"));
    }

    @Test
    void unknownCreditPurposeRespondsBadRequest() throws Exception {
        send(IDENTIFICATION, VALID_REQUEST.replace("DURABLE_GOODS", "CAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revise los campos: destino del crédito"));
    }

    @Test
    void wrongTypeRespondsBadRequestWithItsSpanishLabel() throws Exception {
        send(IDENTIFICATION, VALID_REQUEST.replace("10000.00", "\"diez mil\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revise los campos: monto solicitado"));
    }

    @Test
    void malformedJsonRespondsBadRequest() throws Exception {
        send(IDENTIFICATION, "{\"requestedAmount\": 10000.00, \"termMonths\": 12")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud no es un JSON válido"));
    }

    @Test
    void missingIdentificationHeaderRespondsBadRequest() throws Exception {
        send(null, VALID_REQUEST)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Identificación inválida"));

        verify(loanApplicationService, never()).register(anyString(), any());
    }

    @Test
    void malformedIdentificationRespondsBadRequest() throws Exception {
        send("123", VALID_REQUEST)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La cabecera X-Identificacion debe ser una cédula de 10 dígitos"));

        verify(loanApplicationService, never()).register(anyString(), any());
    }

    @Test
    void amountOutOfRangeRespondsConflictWithTheAllowedRange() throws Exception {
        when(loanApplicationService.register(eq(IDENTIFICATION), any()))
                .thenThrow(new LoanApplicationExceptions.AmountOutOfRange(new BigDecimal("500.00"), new BigDecimal("30000.00")));

        send(IDENTIFICATION, VALID_REQUEST)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("La solicitud no cumple una regla de negocio"))
                .andExpect(jsonPath("$.detail").value("El monto debe estar entre USD 500.00 y USD 30000.00"));
    }

    @Test
    void unexpectedErrorRespondsInternalServerErrorWithoutTechnicalDetails() throws Exception {
        when(loanApplicationService.register(eq(IDENTIFICATION), any()))
                .thenThrow(new UnsupportedOperationException("Pendiente: CustomerService del módulo liabilities (P4)"));

        send(IDENTIFICATION, VALID_REQUEST)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Error interno"))
                .andExpect(jsonPath("$.detail").value(
                        "Ocurrió un error inesperado al procesar la solicitud; intente nuevamente más tarde"));
    }

    @Test
    void nonJsonBodyKeepsItsHttpStatus() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("hola")
                        .header("X-Identificacion", IDENTIFICATION))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void pendingApplicationRespondsConflict() throws Exception {
        when(loanApplicationService.register(eq(IDENTIFICATION), any()))
                .thenThrow(new LoanApplicationExceptions.PendingApplication());

        send(IDENTIFICATION, VALID_REQUEST)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("solicitud pendiente")));
    }
}
