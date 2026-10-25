package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationService;
import com.grupob.bancobanquito.amortization.CalculatedInstallment;
import com.grupob.bancobanquito.liabilities.LiabilitiesService;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomerApplicationService {

    private static final String AUDIT_USER = "system";
    private static final String MIN_AMOUNT = "PAR-01";
    private static final String MAX_AMOUNT = "PAR-02";
    private static final String MIN_TERM = "PAR-03";
    private static final String MAX_TERM = "PAR-04";
    private static final String REFERENCE_RATE = "PAR-06";
    private static final String INSURANCE_RATE = "PAR-07";
    private static final String INSTALLMENT_LIMIT = "PAR-09";
    private static final Locale ECUADOR = Locale.of("es", "EC");

    private final LoanApplicationRepository loanApplicationRepository;
    private final CustomerLoanRepository customerLoanRepository;
    private final LiabilitiesService liabilitiesService;
    private final ParameterService parameterService;
    private final BusinessDateService businessDateService;
    private final AmortizationService amortizationService;

    CustomerApplicationService(LoanApplicationRepository loanApplicationRepository,
                               CustomerLoanRepository customerLoanRepository, LiabilitiesService liabilitiesService,
                               ParameterService parameterService, BusinessDateService businessDateService,
                               AmortizationService amortizationService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.customerLoanRepository = customerLoanRepository;
        this.liabilitiesService = liabilitiesService;
        this.parameterService = parameterService;
        this.businessDateService = businessDateService;
        this.amortizationService = amortizationService;
    }

    @Transactional
    LoanApplication register(LoanApplicationRequest request) {
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        validateRanges(request, businessDate);
        if (liabilitiesService.findCustomer(request.customerId()).isEmpty()) {
            throw new OriginationException(HttpStatus.NOT_FOUND, "No existe el cliente " + request.customerId() + ".");
        }
        if (customerLoanRepository.existsByCustomerIdAndDaysPastDueGreaterThan(request.customerId(), 0)) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Tienes una operación con días de mora, por eso no puedes registrar una nueva solicitud.");
        }
        if (loanApplicationRepository.existsByCustomerIdAndStatus(request.customerId(), LoanApplicationStatus.REGISTERED)) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Ya tienes una solicitud pendiente de revisión.");
        }

        BigDecimal installment = highestInstallment(request, businessDate);
        BigDecimal remaining = request.monthlyIncome().subtract(request.monthlyExpenses());
        BigDecimal limit = parameterService.getValue(INSTALLMENT_LIMIT, businessDate);

        LoanApplication application = new LoanApplication();
        application.setCustomerId(request.customerId());
        application.setAmortizationMethod(request.amortizationMethod());
        application.setRequestedAmount(request.amount());
        application.setTermMonths(request.termMonths());
        application.setPurpose(request.purpose());
        application.setMonthlyIncome(request.monthlyIncome());
        application.setMonthlyExpenses(request.monthlyExpenses());
        application.setApplicationDate(businessDate);
        application.setRegisteredAt(LocalDateTime.now());
        application.setReferenceInstallment(installment);
        application.setStatus(LoanApplicationStatus.REGISTERED);
        application.setCreatedBy(AUDIT_USER);
        application.setCreatedAt(LocalDateTime.now());

        if (remaining.signum() <= 0) {
            reject(application, businessDate, "Tu remanente mensual (ingresos menos gastos) es cero o negativo.");
        } else {
            BigDecimal maxInstallment = remaining.multiply(limit).setScale(2, RoundingMode.DOWN);
            if (installment.compareTo(maxInstallment) > 0) {
                reject(application, businessDate, "La cuota proyectada de USD " + money(installment)
                        + " supera la cuota máxima admisible de USD " + money(maxInstallment) + " ("
                        + limit.movePointRight(2).stripTrailingZeros().toPlainString()
                        + " % de tu remanente mensual).");
            }
        }
        return loanApplicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    Page<LoanApplication> findByCustomer(Integer customerId, LoanApplicationStatus status, Pageable pageable) {
        return status == null
                ? loanApplicationRepository.findByCustomerId(customerId, pageable)
                : loanApplicationRepository.findByCustomerIdAndStatus(customerId, status, pageable);
    }

    private void validateRanges(LoanApplicationRequest request, LocalDate businessDate) {
        BigDecimal minAmount = parameterService.getValue(MIN_AMOUNT, businessDate);
        BigDecimal maxAmount = parameterService.getValue(MAX_AMOUNT, businessDate);
        if (request.amount().compareTo(minAmount) < 0 || request.amount().compareTo(maxAmount) > 0) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT, "El monto debe estar entre USD "
                    + money(minAmount) + " y USD " + money(maxAmount) + ".");
        }
        int minTerm = parameterService.getValue(MIN_TERM, businessDate).intValue();
        int maxTerm = parameterService.getValue(MAX_TERM, businessDate).intValue();
        if (request.termMonths() < minTerm || request.termMonths() > maxTerm) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "El plazo debe estar entre " + minTerm + " y " + maxTerm + " meses.");
        }
    }

    private BigDecimal highestInstallment(LoanApplicationRequest request, LocalDate businessDate) {
        List<CalculatedInstallment> installments = amortizationService.calculate(request.amount(),
                parameterService.getValue(REFERENCE_RATE, businessDate), request.termMonths(),
                request.amortizationMethod(), businessDate);
        BigDecimal insurance = request.amount().multiply(parameterService.getValue(INSURANCE_RATE, businessDate))
                .setScale(2, RoundingMode.HALF_UP);
        return installments.stream()
                .map(installment -> installment.principal().add(installment.interest()))
                .max(Comparator.naturalOrder())
                .orElseThrow()
                .add(insurance);
    }

    private void reject(LoanApplication application, LocalDate businessDate, String reason) {
        application.setStatus(LoanApplicationStatus.REJECTED);
        application.setResolutionDate(businessDate);
        application.setRejectionReason(reason);
    }

    private String money(BigDecimal value) {
        return String.format(ECUADOR, "%,.2f", value);
    }
}
