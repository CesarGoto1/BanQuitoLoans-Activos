package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import com.grupob.bancobanquito.amortization.AmortizationService;
import com.grupob.bancobanquito.liabilities.LiabilitiesService;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static final String SOLCA_RATE = "PAR-08";
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
    LoanApplication register(Integer customerId, LoanApplicationRequest request) {
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        validateRanges(request.amount(), request.termMonths(), businessDate);
        if (liabilitiesService.findCustomer(customerId).isEmpty()) {
            throw new OriginationException(HttpStatus.NOT_FOUND, "No existe el cliente " + customerId + ".");
        }
        if (customerLoanRepository.existsByCustomerIdAndDaysPastDueGreaterThan(customerId, 0)) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Tienes una operación con días de mora, por eso no puedes registrar una nueva solicitud.");
        }
        if (loanApplicationRepository.existsByCustomerIdAndStatus(customerId, LoanApplicationStatus.REGISTERED)) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Ya tienes una solicitud pendiente de revisión.");
        }

        BigDecimal installment = simulate(request.amount(), request.termMonths(), request.amortizationMethod(),
                businessDate).highestInstallment();
        BigDecimal remaining = request.monthlyIncome().subtract(request.monthlyExpenses());
        BigDecimal limit = parameterService.getValue(INSTALLMENT_LIMIT, businessDate);

        LoanApplication application = new LoanApplication();
        application.setCustomerId(customerId);
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

    Integer customerIdOf(String nationalId) {
        return liabilitiesService.findCustomerByNationalId(nationalId)
                .map(customer -> customer.customerId())
                .orElseThrow(() -> new OriginationException(HttpStatus.NOT_FOUND,
                        "No existe un cliente con la cédula " + nationalId + "."));
    }

    @Transactional(readOnly = true)
    LoanSimulation simulate(LoanSimulationRequest request) {
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        validateRanges(request.amount(), request.termMonths(), businessDate);
        return simulate(request.amount(), request.termMonths(), request.amortizationMethod(), businessDate);
    }

    @Transactional(readOnly = true)
    Page<LoanApplication> findByCustomer(Integer customerId, LoanApplicationStatus status, Pageable pageable) {
        return status == null
                ? loanApplicationRepository.findByCustomerId(customerId, pageable)
                : loanApplicationRepository.findByCustomerIdAndStatus(customerId, status, pageable);
    }

    @Transactional(readOnly = true)
    CreditLimits findCreditLimits() {
        return creditLimits(businessDateService.getCurrentBusinessDate());
    }

    private CreditLimits creditLimits(LocalDate businessDate) {
        return new CreditLimits(parameterService.getValue(MIN_AMOUNT, businessDate).setScale(2, RoundingMode.HALF_UP),
                parameterService.getValue(MAX_AMOUNT, businessDate).setScale(2, RoundingMode.HALF_UP),
                parameterService.getValue(MIN_TERM, businessDate).intValue(),
                parameterService.getValue(MAX_TERM, businessDate).intValue());
    }

    private void validateRanges(BigDecimal amount, Integer termMonths, LocalDate businessDate) {
        CreditLimits limits = creditLimits(businessDate);
        if (amount.compareTo(limits.minAmount()) < 0 || amount.compareTo(limits.maxAmount()) > 0) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT, "El monto debe estar entre USD "
                    + money(limits.minAmount()) + " y USD " + money(limits.maxAmount()) + ".");
        }
        if (termMonths < limits.minTermMonths() || termMonths > limits.maxTermMonths()) {
            throw new OriginationException(HttpStatus.UNPROCESSABLE_CONTENT, "El plazo debe estar entre "
                    + limits.minTermMonths() + " y " + limits.maxTermMonths() + " meses.");
        }
    }

    private LoanSimulation simulate(BigDecimal amount, Integer termMonths, AmortizationMethod method,
                                    LocalDate businessDate) {
        BigDecimal rate = parameterService.getValue(REFERENCE_RATE, businessDate);
        BigDecimal insurance = amount.multiply(parameterService.getValue(INSURANCE_RATE, businessDate))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal solca = amount.multiply(parameterService.getValue(SOLCA_RATE, businessDate))
                .setScale(2, RoundingMode.HALF_UP);
        List<SimulatedInstallment> installments = amortizationService
                .calculate(amount, rate, termMonths, method, businessDate).stream()
                .map(installment -> new SimulatedInstallment(installment.installmentNumber(), installment.dueDate(),
                        installment.principal(), installment.interest(), insurance,
                        installment.principal().add(installment.interest()).add(insurance),
                        installment.remainingPrincipalBalance()))
                .toList();
        BigDecimal totalInterest = installments.stream().map(SimulatedInstallment::interest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInsurance = insurance.multiply(BigDecimal.valueOf(termMonths));
        return new LoanSimulation(amount, termMonths, method, rate, nominalRate(rate), insurance, solca,
                amount.subtract(solca), installments.getFirst().total(), installments.getLast().total(),
                totalInterest, totalInsurance, totalInterest.add(totalInsurance).add(solca), installments);
    }

    private BigDecimal nominalRate(BigDecimal effectiveAnnualRate) {
        return amortizationService.monthlyRate(effectiveAnnualRate).multiply(BigDecimal.valueOf(12))
                .setScale(6, RoundingMode.HALF_UP);
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
