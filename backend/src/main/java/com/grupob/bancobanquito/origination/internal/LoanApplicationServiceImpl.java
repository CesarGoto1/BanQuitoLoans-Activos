package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.LoanApplicationService;
import com.grupob.bancobanquito.origination.internal.LoanApplication.ApplicationStatus;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final LoanApplicationRepository loanApplicationRepository;
    private final ParameterService parameterService;
    private final BusinessDateService businessDateService;

    LoanApplicationServiceImpl(LoanApplicationRepository loanApplicationRepository, ParameterService parameterService,
                               BusinessDateService businessDateService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.parameterService = parameterService;
        this.businessDateService = businessDateService;
    }

    @Override
    public LoanApplicationResponse register(String identification, RegisterLoanApplicationRequest request) {
        validateRanges(request.requestedAmount(), request.termMonths());
        Integer customerId = customerIdByIdentification(identification);
        validateNoLoansInArrears(customerId);
        validateNoPendingApplication(customerId);

        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        BigDecimal referenceRate = parameterService.getValue("PAR-06");
        BigDecimal installment = projectedInstallment(request, referenceRate);
        Evaluation evaluation = evaluate(request.monthlyIncome(), request.monthlyExpenses(), installment);

        LoanApplication application = new LoanApplication(customerId, request.amortizationSystem(),
                request.requestedAmount(), request.termMonths(), request.creditPurpose(), request.monthlyIncome(),
                request.monthlyExpenses(), businessDate, businessDate.atTime(LocalTime.now()), installment);
        if (!evaluation.passes()) {
            application.reject(evaluation.rejectionReason(), businessDate);
        }
        application = loanApplicationRepository.save(application);
        log.info("Solicitud {} del cliente {}: {}", application.getId(), mask(identification), application.getStatus());

        return new LoanApplicationResponse(application.getId(), application.getStatus(), businessDate, referenceRate,
                installment, evaluation.monthlySurplus(), evaluation.maxAllowedInstallment(),
                evaluation.surplusPercentage(), application.getRejectionReason());
    }

    void validateRanges(BigDecimal amount, int termMonths) {
        BigDecimal minimumAmount = parameterService.getValue("PAR-01").setScale(2, RoundingMode.HALF_UP);
        BigDecimal maximumAmount = parameterService.getValue("PAR-02").setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(minimumAmount) < 0 || amount.compareTo(maximumAmount) > 0) {
            throw new LoanApplicationExceptions.AmountOutOfRange(minimumAmount, maximumAmount);
        }
        int minimumTerm = parameterService.getValue("PAR-03").intValue();
        int maximumTerm = parameterService.getValue("PAR-04").intValue();
        if (termMonths < minimumTerm || termMonths > maximumTerm) {
            throw new LoanApplicationExceptions.TermOutOfRange(minimumTerm, maximumTerm);
        }
    }

    void validateNoPendingApplication(Integer customerId) {
        if (loanApplicationRepository.existsByCustomerIdAndStatus(customerId, ApplicationStatus.REGISTERED)) {
            throw new LoanApplicationExceptions.PendingApplication();
        }
    }

    Evaluation evaluate(BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal installment) {
        BigDecimal surplus = monthlyIncome.subtract(monthlyExpenses).setScale(2, RoundingMode.HALF_UP);
        BigDecimal limit = parameterService.getValue("PAR-09");
        BigDecimal maxInstallment = surplus.max(BigDecimal.ZERO).multiply(limit).setScale(2, RoundingMode.HALF_UP);

        if (surplus.signum() <= 0) {
            return new Evaluation(surplus, maxInstallment, null, false,
                    "El remanente mensual (ingresos menos gastos) es " + surplus.toPlainString()
                            + "; debe ser mayor que cero");
        }
        BigDecimal percentage = installment.multiply(ONE_HUNDRED).divide(surplus, 2, RoundingMode.HALF_UP);
        if (installment.compareTo(maxInstallment) > 0) {
            return new Evaluation(surplus, maxInstallment, percentage, false,
                    "La cuota proyectada (USD " + installment.toPlainString() + ") supera el "
                            + limit.multiply(ONE_HUNDRED).stripTrailingZeros().toPlainString()
                            + " % del remanente; cuota máxima admisible USD " + maxInstallment.toPlainString());
        }
        return new Evaluation(surplus, maxInstallment, percentage, true, null);
    }

    record Evaluation(BigDecimal monthlySurplus, BigDecimal maxAllowedInstallment, BigDecimal surplusPercentage,
                      boolean passes, String rejectionReason) {
    }

    // Pendiente P4: liabilities.CustomerService (cliente por cédula)
    private Integer customerIdByIdentification(String identification) {
        throw new UnsupportedOperationException("Pendiente: CustomerService del módulo liabilities (P4)");
    }

    // Pendiente P7: loans.LoanService (operaciones con días de mora, RN-35)
    private void validateNoLoansInArrears(Integer customerId) {
        throw new UnsupportedOperationException("Pendiente: LoanService del módulo loans (P7)");
    }

    // Pendiente P1: amortization.AmortizationService (cuota proyectada con la TEA referencial)
    private BigDecimal projectedInstallment(RegisterLoanApplicationRequest request, BigDecimal effectiveAnnualRate) {
        throw new UnsupportedOperationException("Pendiente: AmortizationService del módulo amortization (P1)");
    }

    private static String mask(String identification) {
        return identification.substring(0, 2) + "******" + identification.substring(identification.length() - 2);
    }
}
