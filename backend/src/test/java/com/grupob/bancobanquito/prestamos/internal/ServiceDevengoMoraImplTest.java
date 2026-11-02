package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServiceDevengoMoraImplTest {

    private static final LocalDate DISBURSEMENT = LocalDate.of(2026, 10, 31);
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 11, 30);

    private PrestamoRepository loanRepository;
    private CuotaRepository installmentRepository;
    private DevengoDiarioRepository accrualRepository;
    private MoraDiariaRepository defaultInterestRepository;
    private ServiceAmortizacion amortizationService;
    private ServiceParametro parameterService;
    private ServiceContabilidad accountingService;
    private ServiceDevengoMoraImpl service;

    @BeforeEach
    void setUp() {
        loanRepository = mock(PrestamoRepository.class);
        installmentRepository = mock(CuotaRepository.class);
        accrualRepository = mock(DevengoDiarioRepository.class);
        defaultInterestRepository = mock(MoraDiariaRepository.class);
        amortizationService = mock(ServiceAmortizacion.class);
        parameterService = mock(ServiceParametro.class);
        accountingService = mock(ServiceContabilidad.class);
        service = new ServiceDevengoMoraImpl(loanRepository, installmentRepository, accrualRepository,
                defaultInterestRepository, amortizationService, parameterService, accountingService);
    }

    @Test
    void registraDevengoDesdeElDiaSiguienteAlDesembolso() {
        Prestamo loan = loan(1, EstadoCartera.POR_VENCER);
        when(loanRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(loan));
        when(accrualRepository.existsByLoanIdAndAccrualDate(1, BUSINESS_DATE)).thenReturn(false);
        when(parameterService.getValue("PAR-11", BUSINESS_DATE)).thenReturn(new BigDecimal("360.0000"));
        when(amortizationService.monthlyRate(loan.getEffectiveAnnualRate()))
                .thenReturn(new BigDecimal("0.0130035"));

        assertThat(service.executeAccrual(BUSINESS_DATE)).isEqualTo(1);

        verify(accrualRepository).save(any(DevengoDiario.class));
        verify(accountingService).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void noRegistraDevengoElMismoDiaDelDesembolso() {
        Prestamo loan = loan(1, EstadoCartera.POR_VENCER);
        loan.setDisbursementDate(BUSINESS_DATE);
        when(loanRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(loan));

        assertThat(service.executeAccrual(BUSINESS_DATE)).isZero();

        verify(accrualRepository, times(0)).save(any(DevengoDiario.class));
        verify(accountingService, times(0)).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void noDuplicaDevengoCuandoLaFechaYaFueProcesada() {
        Prestamo loan = loan(1, EstadoCartera.POR_VENCER);
        when(loanRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(loan));
        when(accrualRepository.existsByLoanIdAndAccrualDate(1, BUSINESS_DATE)).thenReturn(true);

        assertThat(service.executeAccrual(BUSINESS_DATE)).isZero();

        verify(accrualRepository, times(0)).save(any(DevengoDiario.class));
        verify(accountingService, times(0)).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void registraElAjusteAlVencimientoDeLaCuota() {
        Prestamo loan = loan(1, EstadoCartera.POR_VENCER);
        Cuota installment = installment(10, loan, BUSINESS_DATE, new BigDecimal("130.03"));
        DevengoDiario accrual = new DevengoDiario(loan, LocalDate.of(2026, 11, 30),
                new BigDecimal("130.0350"), "proceso-cierre");
        when(loanRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(loan));
        when(installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(installment));
        when(accrualRepository.findByLoanIdAndAccrualDateBetweenOrderByAccrualDateAsc(
                1, LocalDate.of(2026, 11, 1), BUSINESS_DATE)).thenReturn(List.of(accrual));

        assertThat(service.executeAccrualAdjustments(BUSINESS_DATE)).isEqualTo(1);

        assertThat(installment.getInterestAccrualAdjustment()).isEqualByComparingTo("-0.01");
        ArgumentCaptor<MovimientoContable> captor = ArgumentCaptor.forClass(MovimientoContable.class);
        verify(accountingService).registrarAsiento(captor.capture());
        assertThat(captor.getValue().monto()).isEqualByComparingTo("0.01");
    }

    @Test
    void registraMoraSobreElCapitalPendienteDeLaCuota() {
        Prestamo loan = loan(1, EstadoCartera.VENCIDA);
        Cuota installment = installment(10, loan, LocalDate.of(2026, 11, 30), new BigDecimal("130.03"));
        installment.setPrincipalAmount(new BigDecimal("775.41"));
        when(loanRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(loan));
        when(installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(installment));
        when(defaultInterestRepository.existsByInstallmentIdAndDefaultDate(10, LocalDate.of(2026, 12, 9)))
                .thenReturn(false);
        when(parameterService.getValue("PAR-10", LocalDate.of(2026, 12, 9)))
                .thenReturn(new BigDecimal("1.10"));
        when(parameterService.getValue("PAR-11", LocalDate.of(2026, 12, 9)))
                .thenReturn(new BigDecimal("360.0000"));
        when(amortizationService.monthlyRate(loan.getEffectiveAnnualRate()))
                .thenReturn(new BigDecimal("0.0130035"));

        assertThat(service.executeDefaultInterest(LocalDate.of(2026, 12, 9))).isEqualTo(1);

        assertThat(installment.getAccruedDefaultInterest()).isEqualByComparingTo("0.3697");
        verify(defaultInterestRepository).save(any(MoraDiaria.class));
    }

    @Test
    void procesaCadaCuotaVencidaDeLaOperacion() {
        LocalDate businessDate = LocalDate.of(2026, 12, 9);
        Prestamo loan = loan(1, EstadoCartera.VENCIDA);
        Cuota first = installment(10, loan, LocalDate.of(2026, 11, 30), new BigDecimal("130.03"));
        Cuota second = installment(11, loan, LocalDate.of(2026, 11, 30), new BigDecimal("119.95"));
        when(loanRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(loan));
        when(installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(first, second));
        when(defaultInterestRepository.existsByInstallmentIdAndDefaultDate(anyInt(), eq(businessDate)))
                .thenReturn(false);
        when(parameterService.getValue("PAR-10", businessDate)).thenReturn(new BigDecimal("1.10"));
        when(parameterService.getValue("PAR-11", businessDate)).thenReturn(new BigDecimal("360.0000"));
        when(amortizationService.monthlyRate(loan.getEffectiveAnnualRate()))
                .thenReturn(new BigDecimal("0.0130035"));

        assertThat(service.executeDefaultInterest(businessDate)).isEqualTo(2);

        verify(defaultInterestRepository, times(2)).save(any(MoraDiaria.class));
        verify(accountingService, times(2)).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void noRegistraMoraParaUnaCuotaPagada() {
        LocalDate businessDate = LocalDate.of(2026, 12, 9);
        Prestamo loan = loan(1, EstadoCartera.VENCIDA);
        Cuota installment = installment(10, loan, LocalDate.of(2026, 11, 30), new BigDecimal("130.03"));
        installment.setStatus(EstadoCuota.PAGADA);
        when(loanRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(loan));
        when(installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(installment));
        when(parameterService.getValue("PAR-10", businessDate)).thenReturn(new BigDecimal("1.10"));
        when(parameterService.getValue("PAR-11", businessDate)).thenReturn(new BigDecimal("360.0000"));

        assertThat(service.executeDefaultInterest(businessDate)).isZero();

        verify(defaultInterestRepository, times(0)).save(any(MoraDiaria.class));
        verify(accountingService, times(0)).registrarAsiento(any(MovimientoContable.class));
    }

    private Prestamo loan(Integer id, EstadoCartera status) {
        Prestamo loan = new Prestamo();
        loan.setId(id);
        loan.setDisbursementDate(DISBURSEMENT);
        loan.setEffectiveAnnualRate(new BigDecimal("0.1677"));
        loan.setPrincipalBalance(new BigDecimal("9224.59"));
        loan.setPortfolioStatus(status);
        return loan;
    }

    private Cuota installment(Integer id, Prestamo loan, LocalDate dueDate, BigDecimal interest) {
        Cuota installment = new Cuota();
        installment.setId(id);
        installment.setLoan(loan);
        installment.setDueDate(dueDate);
        installment.setInterestAmount(interest);
        installment.setPrincipalAmount(new BigDecimal("775.41"));
        installment.setPrincipalPaid(BigDecimal.ZERO);
        installment.setAccruedDefaultInterest(BigDecimal.ZERO.setScale(4));
        installment.setStatus(EstadoCuota.PENDIENTE);
        return installment;
    }
}
