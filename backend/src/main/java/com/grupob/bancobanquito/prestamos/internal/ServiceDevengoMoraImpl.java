package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ServiceDevengoMora;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceDevengoMoraImpl implements ServiceDevengoMora {

    private static final String PARAMETER_MORA_FACTOR = "PAR-10";
    private static final String PARAMETER_DAY_BASE = "PAR-11";
    private static final String AUDIT_USER = "proceso-cierre";
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4);

    private final PrestamoRepository loanRepository;
    private final CuotaRepository installmentRepository;
    private final DevengoDiarioRepository accrualRepository;
    private final MoraDiariaRepository defaultInterestRepository;
    private final ServiceAmortizacion amortizationService;
    private final ServiceParametro parameterService;
    private final ServiceContabilidad accountingService;

    ServiceDevengoMoraImpl(PrestamoRepository loanRepository, CuotaRepository installmentRepository,
                           DevengoDiarioRepository accrualRepository,
                           MoraDiariaRepository defaultInterestRepository,
                           ServiceAmortizacion amortizationService, ServiceParametro parameterService,
                           ServiceContabilidad accountingService) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.accrualRepository = accrualRepository;
        this.defaultInterestRepository = defaultInterestRepository;
        this.amortizationService = amortizationService;
        this.parameterService = parameterService;
        this.accountingService = accountingService;
    }

    @Override
    @Transactional
    public int executeAccrual(LocalDate businessDate) {
        int processed = 0;
        for (Prestamo loan : activeLoans()) {
            if (!businessDate.isAfter(loan.getDisbursementDate())
                    || accrualRepository.existsByLoanIdAndAccrualDate(loan.getId(), businessDate)) {
                continue;
            }

            int dayBase = parameterService.getValue(PARAMETER_DAY_BASE, businessDate).intValueExact();
            BigDecimal dailyInterest = CalculoDevengo.interesDiario(
                    loan.getPrincipalBalance(), nominalRate(loan), dayBase);
            DevengoDiario accrual = new DevengoDiario(loan, businessDate, dailyInterest, AUDIT_USER);
            accrualRepository.save(accrual);
            registerAccrualEntry(loan, businessDate, dailyInterest, accrualType(loan));
            processed++;
        }
        return processed;
    }

    @Override
    @Transactional
    public int executeAccrualAdjustments(LocalDate businessDate) {
        int processed = 0;
        for (Prestamo loan : activeLoans()) {
            List<Cuota> installments = installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(loan.getId());
            for (int index = 0; index < installments.size(); index++) {
                Cuota installment = installments.get(index);
                if (!businessDate.equals(installment.getDueDate())) {
                    continue;
                }

                LocalDate periodStart = index == 0
                        ? loan.getDisbursementDate()
                        : installments.get(index - 1).getDueDate();
                BigDecimal accrued = accrualRepository
                        .findByLoanIdAndAccrualDateBetweenOrderByAccrualDateAsc(
                                loan.getId(), periodStart.plusDays(1), businessDate)
                        .stream()
                        .map(DevengoDiario::getAccruedInterest)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal adjustment = installment.getInterestAmount().subtract(accrued)
                        .setScale(2, RoundingMode.HALF_UP);
                installment.setInterestAccrualAdjustment(adjustment);
                installmentRepository.save(installment);
                registerAccrualAdjustmentEntry(loan, installment, businessDate, adjustment);
                processed++;
            }
        }
        return processed;
    }

    @Override
    @Transactional
    public int executeDefaultInterest(LocalDate businessDate) {
        int processed = 0;
        BigDecimal delinquencyFactor = parameterService.getValue(PARAMETER_MORA_FACTOR, businessDate);
        int dayBase = parameterService.getValue(PARAMETER_DAY_BASE, businessDate).intValueExact();
        for (Prestamo loan : activeLoans()) {
            if (loan.getPortfolioStatus() == EstadoCartera.POR_VENCER) {
                continue;
            }
            for (Cuota installment : installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(loan.getId())) {
                if (!isOverdueUnpaid(installment, businessDate)
                        || defaultInterestRepository.existsByInstallmentIdAndDefaultDate(installment.getId(), businessDate)) {
                    continue;
                }

                BigDecimal overdueCapital = installment.getPrincipalAmount()
                        .subtract(installment.getPrincipalPaid()).max(BigDecimal.ZERO);
                BigDecimal dailyInterest = overdueCapital.signum() == 0
                        ? ZERO
                        : CalculoMora.interesDiario(overdueCapital, nominalRate(loan), delinquencyFactor, dayBase);
                MoraDiaria defaultInterest = new MoraDiaria(installment, businessDate, dailyInterest, AUDIT_USER);
                defaultInterestRepository.save(defaultInterest);
                installment.setAccruedDefaultInterest(installment.getAccruedDefaultInterest().add(dailyInterest));
                installmentRepository.save(installment);
                registerDefaultInterestEntry(loan, installment, businessDate, dailyInterest);
                processed++;
            }
        }
        return processed;
    }

    private List<Prestamo> activeLoans() {
        return loanRepository.findByPortfolioStatusNotIn(List.of(EstadoCartera.CASTIGADA, EstadoCartera.CANCELADA));
    }

    private boolean isOverdueUnpaid(Cuota installment, LocalDate businessDate) {
        return installment.getDueDate().isBefore(businessDate)
                && installment.getStatus() != EstadoCuota.PAGADA;
    }

    private BigDecimal nominalRate(Prestamo loan) {
        return amortizationService.monthlyRate(loan.getEffectiveAnnualRate()).multiply(MONTHS_PER_YEAR);
    }

    private TipoAsiento accrualType(Prestamo loan) {
        return loan.getPortfolioStatus() == EstadoCartera.EN_LITIGIO
                ? TipoAsiento.DEVENGO_SUSPENSO : TipoAsiento.DEVENGO;
    }

    private void registerAccrualEntry(Prestamo loan, LocalDate businessDate, BigDecimal dailyInterest,
                                      TipoAsiento type) {
        BigDecimal amount = dailyInterest.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() == 0) {
            return;
        }
        CodigoCuentaContable debit = type == TipoAsiento.DEVENGO
                ? CodigoCuentaContable.INTERESES_POR_COBRAR
                : CodigoCuentaContable.INTERESES_EN_SUSPENSO;
        CodigoCuentaContable credit = type == TipoAsiento.DEVENGO
                ? CodigoCuentaContable.INGRESOS_INTERESES
                : CodigoCuentaContable.CONTRA_CUENTAS_ORDEN;
        accountingService.registrarAsiento(new MovimientoContable(loan.getId(), businessDate, type,
                debit, credit, amount, "DEVENGO:" + loan.getId() + ":" + businessDate));
    }

    private void registerDefaultInterestEntry(Prestamo loan, Cuota installment, LocalDate businessDate,
                                              BigDecimal dailyInterest) {
        BigDecimal amount = dailyInterest.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() == 0) {
            return;
        }
        boolean suspense = loan.getPortfolioStatus() == EstadoCartera.EN_LITIGIO;
        CodigoCuentaContable debit = suspense
                ? CodigoCuentaContable.INTERESES_EN_SUSPENSO
                : CodigoCuentaContable.INTERESES_MORA_POR_COBRAR;
        CodigoCuentaContable credit = suspense
                ? CodigoCuentaContable.CONTRA_CUENTAS_ORDEN
                : CodigoCuentaContable.INGRESOS_MORA;
        accountingService.registrarAsiento(new MovimientoContable(loan.getId(), businessDate, TipoAsiento.MORA,
                debit, credit, amount, "MORA:" + installment.getId() + ":" + businessDate));
    }

    private void registerAccrualAdjustmentEntry(Prestamo loan, Cuota installment, LocalDate businessDate,
                                                BigDecimal adjustment) {
        if (adjustment.signum() == 0) {
            return;
        }
        boolean suspense = loan.getPortfolioStatus() == EstadoCartera.EN_LITIGIO;
        CodigoCuentaContable incomeDebit = suspense
                ? CodigoCuentaContable.INTERESES_EN_SUSPENSO
                : CodigoCuentaContable.INTERESES_POR_COBRAR;
        CodigoCuentaContable incomeCredit = suspense
                ? CodigoCuentaContable.CONTRA_CUENTAS_ORDEN
                : CodigoCuentaContable.INGRESOS_INTERESES;
        BigDecimal amount = adjustment.abs();
        CodigoCuentaContable debit = adjustment.signum() > 0 ? incomeDebit : incomeCredit;
        CodigoCuentaContable credit = adjustment.signum() > 0 ? incomeCredit : incomeDebit;
        accountingService.registrarAsiento(new MovimientoContable(loan.getId(), businessDate,
                TipoAsiento.AJUSTE_DEVENGO, debit, credit, amount,
                "AJUSTE_DEVENGO:" + installment.getId() + ":" + businessDate));
    }
}
