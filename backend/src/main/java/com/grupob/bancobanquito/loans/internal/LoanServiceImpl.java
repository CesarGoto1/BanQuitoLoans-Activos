package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.amortization.AmortizationService;
import com.grupob.bancobanquito.amortization.CalculatedInstallment;
import com.grupob.bancobanquito.liabilities.Account;
import com.grupob.bancobanquito.liabilities.Customer;
import com.grupob.bancobanquito.liabilities.DisbursementInstruction;
import com.grupob.bancobanquito.liabilities.LiabilitiesService;
import com.grupob.bancobanquito.loans.DisbursementPreview;
import com.grupob.bancobanquito.loans.DisbursementResult;
import com.grupob.bancobanquito.loans.LoanService;
import com.grupob.bancobanquito.loans.PendingDisbursement;
import com.grupob.bancobanquito.loans.EstadoCartera;
import com.grupob.bancobanquito.origination.ApprovedLoanApplication;
import com.grupob.bancobanquito.origination.LoanApplicationService;
import com.grupob.bancobanquito.origination.PendingDisbursementApplication;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LoanServiceImpl implements LoanService {

    private static final String INSURANCE_RATE = "PAR-07";
    private static final String SOLCA_RATE = "PAR-08";
    private static final String AUDIT_USER = "system";
    private static final DateTimeFormatter LOAN_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;
    private final LoanApplicationService loanApplicationService;
    private final AmortizationService amortizationService;
    private final LiabilitiesService liabilitiesService;
    private final ParameterService parameterService;
    private final BusinessDateService businessDateService;

    LoanServiceImpl(LoanRepository loanRepository, InstallmentRepository installmentRepository,
                    LoanApplicationService loanApplicationService, AmortizationService amortizationService,
                    LiabilitiesService liabilitiesService, ParameterService parameterService,
                    BusinessDateService businessDateService) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.loanApplicationService = loanApplicationService;
        this.amortizationService = amortizationService;
        this.liabilitiesService = liabilitiesService;
        this.parameterService = parameterService;
        this.businessDateService = businessDateService;
    }

    @Override
    @Transactional(readOnly = true)
    public DisbursementPreview previewDisbursement(Integer applicationId) {
        ApprovedLoanApplication application = pendingApplication(applicationId);
        Customer customer = customer(application.customerId());
        BigDecimal solcaContribution = solcaContribution(application.approvedAmount());

        return new DisbursementPreview(application.applicationId(), customer.nationalId(),
                customer.firstNames() + " " + customer.lastNames(), application.approvedAmount(),
                application.effectiveAnnualRate(), application.termMonths(), application.amortizationMethod(),
                monthlyInsurance(application.approvedAmount()), solcaContribution,
                application.approvedAmount().subtract(solcaContribution),
                businessDateService.getCurrentBusinessDate(),
                liabilitiesService.findActiveAccounts(application.customerId()));
    }

    @Override
    @Transactional
    public DisbursementResult disburse(Integer applicationId, String destinationAccount) {
        ApprovedLoanApplication application = pendingApplication(applicationId);
        Customer customer = customer(application.customerId());

        List<Account> accounts = liabilitiesService.findActiveAccounts(application.customerId());
        if (accounts.stream().noneMatch(account -> account.accountNumber().equals(destinationAccount))) {
            throw new LoanException(HttpStatus.BAD_REQUEST,
                    "La cuenta " + destinationAccount + " no es una cuenta activa del cliente.");
        }

        LocalDate disbursementDate = businessDateService.getCurrentBusinessDate();
        BigDecimal approvedAmount = application.approvedAmount();
        BigDecimal monthlyInsurance = monthlyInsurance(approvedAmount);
        BigDecimal solcaContribution = solcaContribution(approvedAmount);

        Loan loan = new Loan();
        loan.setApplicationId(application.applicationId());
        loan.setCustomerId(application.customerId());
        loan.setCustomerNationalId(customer.nationalId());
        loan.setAmortizationMethod(application.amortizationMethod());
        loan.setLoanNumber(loanNumber(disbursementDate, applicationId));
        loan.setApprovedAmount(approvedAmount);
        loan.setEffectiveAnnualRate(application.effectiveAnnualRate());
        loan.setTermMonths(application.termMonths());
        loan.setMonthlyInsurance(monthlyInsurance);
        loan.setSolcaContribution(solcaContribution);
        loan.setNetAmount(approvedAmount.subtract(solcaContribution));
        loan.setDestinationAccount(destinationAccount);
        loan.setDisbursementDate(disbursementDate);
        loan.setDisbursementInstructionStatus(DisbursementInstructionStatus.PENDING);
        loan.setPortfolioStatus(EstadoCartera.POR_VENCER);
        loan.setPrincipalBalance(approvedAmount);
        loan.setDaysPastDue(0);
        loan.setCreatedBy(AUDIT_USER);
        loan.setCreatedAt(LocalDateTime.now());
        loanRepository.save(loan);

        List<Installment> installments = new ArrayList<>();
        for (CalculatedInstallment calculated : amortizationService.calculate(approvedAmount,
                application.effectiveAnnualRate(), application.termMonths(), application.amortizationMethod(),
                disbursementDate)) {
            installments.add(newInstallment(loan, calculated, monthlyInsurance));
        }
        installmentRepository.saveAll(installments);
        loanRepository.flush();

        try {
            liabilitiesService.creditDisbursement(new DisbursementInstruction(loan.getLoanNumber(),
                    customer.nationalId(), destinationAccount, approvedAmount, solcaContribution,
                    loan.getNetAmount(), disbursementDate));
        } catch (RuntimeException e) {
            throw new LoanException(HttpStatus.BAD_GATEWAY,
                    "No se pudo acreditar en el Core de Pasivos; el desembolso no se realizó. Intenta de nuevo.");
        }
        loan.setDisbursementInstructionStatus(DisbursementInstructionStatus.DELIVERED);
        loan.setLastModifiedBy(AUDIT_USER);
        loan.setLastModifiedAt(LocalDateTime.now());

        return result(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DisbursementResult> findDisbursement(Integer applicationId) {
        return loanRepository.findByApplicationId(applicationId).map(this::result);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PendingDisbursement> findPendingDisbursements(Pageable pageable) {
        return loanApplicationService.findPendingDisbursement(pageable).map(this::pendingDisbursement);
    }

    private PendingDisbursement pendingDisbursement(PendingDisbursementApplication application) {
        Optional<Customer> customer = liabilitiesService.findCustomer(application.customerId());
        return new PendingDisbursement(application.applicationId(), application.customerId(),
                customer.map(Customer::nationalId).orElse(null),
                customer.map(found -> found.firstNames() + " " + found.lastNames()).orElse(null),
                application.approvedAmount(), application.effectiveAnnualRate(), application.termMonths(),
                application.amortizationMethod(), application.resolutionDate());
    }

    private ApprovedLoanApplication pendingApplication(Integer applicationId) {
        ApprovedLoanApplication application = loanApplicationService.findApproved(applicationId)
                .orElseThrow(() -> new LoanException(HttpStatus.NOT_FOUND,
                        "No existe una solicitud aprobada con el número " + applicationId + "."));
        if (loanRepository.existsByApplicationId(applicationId)) {
            throw new LoanException(HttpStatus.CONFLICT,
                    "La solicitud " + applicationId + " ya fue desembolsada.");
        }
        return application;
    }

    private Customer customer(Integer customerId) {
        return liabilitiesService.findCustomer(customerId)
                .orElseThrow(() -> new LoanException(HttpStatus.NOT_FOUND,
                        "El cliente " + customerId + " no existe en el Core de Pasivos."));
    }

    private BigDecimal monthlyInsurance(BigDecimal approvedAmount) {
        return approvedAmount.multiply(parameterService.getValue(INSURANCE_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal solcaContribution(BigDecimal approvedAmount) {
        return approvedAmount.multiply(parameterService.getValue(SOLCA_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    private String loanNumber(LocalDate disbursementDate, Integer applicationId) {
        return "OP-" + disbursementDate.format(LOAN_NUMBER_DATE) + "-" + String.format("%06d", applicationId);
    }

    private Installment newInstallment(Loan loan, CalculatedInstallment calculated, BigDecimal monthlyInsurance) {
        Installment installment = new Installment();
        installment.setLoan(loan);
        installment.setInstallmentNumber(calculated.installmentNumber());
        installment.setDueDate(calculated.dueDate());
        installment.setPrincipalAmount(calculated.principal());
        installment.setInterestAmount(calculated.interest());
        installment.setInsuranceAmount(monthlyInsurance);
        installment.setRemainingPrincipalBalance(calculated.remainingPrincipalBalance());
        installment.setInsurancePaid(BigDecimal.ZERO);
        installment.setDefaultInterestPaid(BigDecimal.ZERO);
        installment.setInterestPaid(BigDecimal.ZERO);
        installment.setPrincipalPaid(BigDecimal.ZERO);
        installment.setAccruedDefaultInterest(BigDecimal.ZERO);
        installment.setStatus(InstallmentStatus.PENDING);
        installment.setCreatedBy(AUDIT_USER);
        installment.setCreatedAt(loan.getCreatedAt());
        return installment;
    }

    private DisbursementResult result(Loan loan) {
        return new DisbursementResult(loan.getApplicationId(), loan.getLoanNumber(), loan.getNetAmount(),
                loan.getDestinationAccount(), loan.getDisbursementDate());
    }
}
