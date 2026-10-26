package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.*;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
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
import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;

class LoanDecisionServiceTest {

    private final SolicitudRepository applications = mock(SolicitudRepository.class);
    private final ServiceAnalista analysts = mock(ServiceAnalista.class);
    private final ServiceParametro parametros = mock(ServiceParametro.class);
    private final ServiceFechaNegocio dates = mock(ServiceFechaNegocio.class);
    private final ServiceAmortizacion amortizacion = mock(ServiceAmortizacion.class);
    private final LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
    private final ServiceDecisionPrestamoImpl service = new ServiceDecisionPrestamoImpl(applications, analysts,
            new PoliticaAprobacionPrestamo(), parametros, dates, amortizacion);
    private Solicitud application;

    @BeforeEach
    void setUp() {
        when(analysts.findByNationalId("1710000001"))
                .thenReturn(Optional.of(new SesionAnalista(42, "1710000001", "Analista de pruebas")));
        application = new Solicitud(7);
        application.setStatus(EstadoSolicitud.REGISTRADA);
        application.setRequestedAmount(new BigDecimal("10000.00"));
        application.setTermMonths(12);
        application.setAmortizationMethod(MetodoAmortizacion.FRANCES);
        application.setMonthlyIncome(new BigDecimal("3500.00"));
        application.setMonthlyExpenses(new BigDecimal("1200.00"));
        application.setReferenceInstallment(new BigDecimal("100.00"));
        application.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        application.setCreatedBy("customer");
        when(applications.findForDecision(7)).thenReturn(Optional.of(application));
        when(dates.getCurrentBusinessDate()).thenReturn(fechaNegocio);
        when(parametros.getValue("PAR-05", fechaNegocio)).thenReturn(new BigDecimal("0.1677"));
        when(parametros.getValue("PAR-07", fechaNegocio)).thenReturn(new BigDecimal("0.0006"));
        when(parametros.getValue("PAR-09", fechaNegocio)).thenReturn(new BigDecimal("0.4000"));
    }

    private void schedule(String monto) {
        var installment = new CuotaCalculada(1, fechaNegocio.plusMonths(1),
                new BigDecimal(monto).subtract(new BigDecimal("136.03")), new BigDecimal("130.03"),
                new BigDecimal("9224.59"));
        when(amortizacion.calculate(any(), any(), any(), any(), any())).thenReturn(List.of(installment));
    }

    @Test
    void approvesWithFinalRateInsuranceBusinessDateAndAudit() {
        schedule("911.44");
        var result = service.approveApplication(7, "1710000001", new BigDecimal("0.1677"));
        assertThat(result.status()).isEqualTo("APROBADA");
        assertThat(result.finalInstallment()).isEqualByComparingTo("911.44");
        assertThat(result.teaAsignada()).isEqualByComparingTo("0.1677");
        assertThat(result.fechaResolucion()).isEqualTo(fechaNegocio);
        assertThat(result.idAnalista()).isEqualTo(42);
        assertThat(application.getLastModifiedBy()).isEqualTo("1710000001");
        assertThat(application.getLastModifiedAt()).isAfter(application.getCreatedAt());
        assertThat(application.getCreatedBy()).isEqualTo("customer");
        verify(amortizacion).calculate(new BigDecimal("10000.00"), new BigDecimal("0.1677"), 12,
                MetodoAmortizacion.FRANCES, fechaNegocio);
        verify(applications).saveAndFlush(application);
        verify(dates, times(1)).getCurrentBusinessDate();
    }

    @Test
    void rejectsWithoutDependingOnAmortizationOrFinancialParameters() {
        var result = service.rejectApplication(7, "1710000001", "  Capacidad insuficiente.  ");
        assertThat(result.status()).isEqualTo("RECHAZADA");
        assertThat(result.motivoRechazo()).isEqualTo("Capacidad insuficiente.");
        assertThat(result.teaAsignada()).isNull();
        assertThat(result.finalInstallment()).isNull();
        assertThat(result.fechaResolucion()).isEqualTo(fechaNegocio);
        assertThat(result.idAnalista()).isEqualTo(42);
        verifyNoInteractions(amortizacion);
        verify(parametros, never()).getValue(anyString(), any());
        verify(applications).saveAndFlush(application);
    }

    @Test
    void rejectsRateAboveMaximumBeforeCalculating() {
        assertThatThrownBy(() -> service.approveApplication(7, "1710000001", new BigDecimal("0.18")))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
        verifyNoInteractions(amortizacion);
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void repeatsCapacityEvaluationWithTheDefinitiveInstallment() {
        schedule("969.36");
        assertThatThrownBy(() -> service.approveApplication(7, "1710000001", new BigDecimal("0.1677")))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
        assertThat(application.getStatus()).isEqualTo(EstadoSolicitud.REGISTRADA);
        verify(applications, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = {"APROBADA", "RECHAZADA"})
    void refusesBothDecisionsForResolvedApplications(EstadoSolicitud status) {
        application.setStatus(status);
        assertThatThrownBy(() -> service.approveApplication(7, "1710000001", new BigDecimal("0.16")))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000001", "Motivo válido"))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
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
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
    }

    @Test
    void refusesLongRejectionReason() {
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000001", "a".repeat(201)))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
    }

    @Test
    void propagatesPersistenceFailure() {
        doThrow(new IllegalStateException("Database failure")).when(applications).saveAndFlush(any());
        assertThatThrownBy(() -> service.rejectApplication(7, "1710000001", "Motivo válido"))
                .isInstanceOf(IllegalStateException.class);
    }
}
