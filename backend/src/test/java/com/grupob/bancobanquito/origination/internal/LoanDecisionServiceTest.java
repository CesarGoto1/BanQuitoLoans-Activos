package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.*;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoanDecisionServiceTest {

    private final SolicitudRepository applications = mock(SolicitudRepository.class);
    private final AnalystRepository analysts = mock(AnalystRepository.class);
    private final ParameterService parameters = mock(ParameterService.class);
    private final BusinessDateService dates = mock(BusinessDateService.class);
    private final AmortizacionService amortization = mock(AmortizacionService.class);
    private final LocalDate businessDate = LocalDate.of(2026, 10, 31);
    private final LoanDecisionServiceImpl service = new LoanDecisionServiceImpl(applications, analysts,
            new LoanApprovalPolicy(), parameters, dates, amortization);
    private Solicitud application;

    @BeforeEach
    void setUp() {
        Analyst analyst = mock(Analyst.class);
        when(analyst.getId()).thenReturn(42);
        when(analysts.findByIdentification("1710000001")).thenReturn(Optional.of(analyst));
        application = new Solicitud(7);
        application.setEstadoSolicitud(EstadoSolicitud.REGISTRADA);
        application.setMontoSolicitado(new BigDecimal("10000.00"));
        application.setPlazoMeses(12);
        application.setSistemaAmortizacion("FRANCES");
        application.setIngresosMensuales(new BigDecimal("3500.00"));
        application.setGastosMensuales(new BigDecimal("1200.00"));
        application.setCuotaEvaluacionReferencial(new BigDecimal("100.00"));
        application.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 0, 0));
        application.setUsuarioCreacion("customer");
        when(applications.findForDecision(7)).thenReturn(Optional.of(application));
        when(dates.getCurrentBusinessDate()).thenReturn(businessDate);
        when(parameters.getValue("PAR-05", businessDate)).thenReturn(new BigDecimal("0.1677"));
        when(parameters.getValue("PAR-07", businessDate)).thenReturn(new BigDecimal("0.0006"));
        when(parameters.getValue("PAR-09", businessDate)).thenReturn(new BigDecimal("0.4000"));
    }

    private void schedule(String amount) {
        var installment = new CuotaAmortizacion(1, businessDate.plusMonths(1), new BigDecimal("775.41"),
                new BigDecimal("130.03"), new BigDecimal("6.00"), new BigDecimal(amount), new BigDecimal("9224.59"));
        when(amortization.generarTabla(any())).thenReturn(new TablaAmortizacion(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(installment)));
    }

    @Test
    void approvesWithFinalRateInsuranceBusinessDateAndAudit() {
        schedule("911.44");
        var result = service.approveApplication(7, "1710000001", new BigDecimal("0.1677"));
        assertThat(result.status()).isEqualTo("APROBADA");
        assertThat(result.finalInstallment()).isEqualByComparingTo("911.44");
        assertThat(result.annualEffectiveRate()).isEqualByComparingTo("0.1677");
        assertThat(result.resolutionDate()).isEqualTo(businessDate);
        assertThat(result.analystId()).isEqualTo(42);
        assertThat(application.getUsuarioModificacion()).isEqualTo("1710000001");
        assertThat(application.getFechaModificacion()).isAfter(application.getFechaCreacion());
        assertThat(application.getUsuarioCreacion()).isEqualTo("customer");
        verify(amortization).generarTabla(new CondicionesAmortizacion(new BigDecimal("10000.00"), 12,
                new BigDecimal("0.1677"), new BigDecimal("6.00"), businessDate, SistemaAmortizacion.FRANCES));
        verify(applications).saveAndFlush(application);
        verify(dates, times(1)).getCurrentBusinessDate();
    }

    @Test
    void rejectsWithoutDependingOnAmortizationOrFinancialParameters() {
        var result = service.rejectApplication(7, "1710000001", "  Capacidad insuficiente.  ");
        assertThat(result.status()).isEqualTo("RECHAZADA");
        assertThat(result.rejectionReason()).isEqualTo("Capacidad insuficiente.");
        assertThat(result.annualEffectiveRate()).isNull();
        assertThat(result.finalInstallment()).isNull();
        assertThat(result.resolutionDate()).isEqualTo(businessDate);
        assertThat(result.analystId()).isEqualTo(42);
        verifyNoInteractions(amortization);
        verify(parameters, never()).getValue(anyString(), any());
        verify(applications).saveAndFlush(application);
    }

    @Test
    void rejectsRateAboveMaximumBeforeCalculating() {
        assertThatThrownBy(() -> service.approveApplication(7, "1710000001", new BigDecimal("0.18")))
                .isInstanceOf(LoanDecisionRuleException.class);
        verifyNoInteractions(amortization);
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void repeatsCapacityEvaluationWithTheDefinitiveInstallment() {
        schedule("969.36");
        assertThatThrownBy(() -> service.approveApplication(7, "1710000001", new BigDecimal("0.1677")))
                .isInstanceOf(LoanDecisionRuleException.class);
        assertThat(application.getEstadoSolicitud()).isEqualTo(EstadoSolicitud.REGISTRADA);
        verify(applications, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = {"APROBADA", "RECHAZADA"})
    void refusesBothDecisionsForResolvedApplications(EstadoSolicitud status) {
        application.setEstadoSolicitud(status);
        assertThatThrownBy(() -> service.approveApplication(7, "1710000001", new BigDecimal("0.16")))
                .isInstanceOf(LoanDecisionRuleException.class);
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000001", "Motivo válido"))
                .isInstanceOf(LoanDecisionRuleException.class);
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void refusesUnknownApplication() {
        assertThatThrownBy(() -> service.rejectApplication(999, "1710000001", "Motivo válido"))
                .isInstanceOf(SolicitudNoEncontradaException.class);
    }

    @Test
    void refusesUnknownAnalyst() {
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000002", "Motivo válido"))
                .isInstanceOf(AccesoDenegadoAnalistaException.class);
        verify(applications, never()).findForDecision(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abc", "123"})
    void refusesInvalidIdentification(String identification) {
        assertThatThrownBy(() -> service.rejectApplication(7, identification, "Motivo válido"))
                .isInstanceOf(CabeceraFaltanteException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void refusesBlankRejectionReason(String reason) {
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000001", reason))
                .isInstanceOf(LoanDecisionRuleException.class);
    }

    @Test
    void refusesLongRejectionReason() {
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000001", "a".repeat(201)))
                .isInstanceOf(LoanDecisionRuleException.class);
    }

    @Test
    void propagatesPersistenceFailure() {
        doThrow(new IllegalStateException("Database failure")).when(applications).saveAndFlush(any());
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000001", "Motivo válido"))
                .isInstanceOf(IllegalStateException.class);
    }
}
