package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationService;
import com.grupob.bancobanquito.origination.LoanDecisionResult;
import com.grupob.bancobanquito.origination.LoanDecisionService;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.grupob.bancobanquito.access.AnalystService;
import com.grupob.bancobanquito.access.AnalystSession;

@Service
@Transactional
class LoanDecisionServiceImpl implements LoanDecisionService {

    private static final LoanApplicationStatusConverter STATUS = new LoanApplicationStatusConverter();

    private final LoanApplicationRepository applicationRepository;
    private final AnalystService analystService;
    private final LoanApprovalPolicy approvalPolicy;
    private final ParameterService parameterService;
    private final BusinessDateService businessDateService;
    private final AmortizationService amortizationService;

    LoanDecisionServiceImpl(LoanApplicationRepository applicationRepository, AnalystService analystService,
            LoanApprovalPolicy approvalPolicy, ParameterService parameterService,
            BusinessDateService businessDateService, AmortizationService amortizationService) {
        this.applicationRepository = applicationRepository;
        this.analystService = analystService;
        this.approvalPolicy = approvalPolicy;
        this.parameterService = parameterService;
        this.businessDateService = businessDateService;
        this.amortizationService = amortizationService;
    }

    @Override
    public LoanDecisionResult approveApplication(Integer applicationId, String identification,
            BigDecimal annualEffectiveRate) {
        Integer analystId = resolveAnalyst(identification);
        LoanApplication application = loadRegisteredApplication(applicationId);
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        approvalPolicy.validateAnnualEffectiveRate(annualEffectiveRate,
                parameterService.getValue("PAR-05", businessDate));
        if (annualEffectiveRate.stripTrailingZeros().scale() > 4
                || annualEffectiveRate.compareTo(BigDecimal.TEN) >= 0) {
            throw new LoanDecisionRuleException("La tasa debe tener máximo un entero y cuatro decimales.");
        }
        BigDecimal installment = calculateInstallment(application, annualEffectiveRate, businessDate);
        approvalPolicy.validatePaymentCapacity(application.getMonthlyIncome(), application.getMonthlyExpenses(),
                installment, parameterService.getValue("PAR-09", businessDate));
        application.setAssignedRate(annualEffectiveRate.setScale(4, RoundingMode.UNNECESSARY));
        application.setFinalInstallment(installment);
        application.setRejectionReason(null);
        application.setStatus(LoanApplicationStatus.APPROVED);
        return saveDecision(application, analystId, identification, businessDate);
    }

    @Override
    public LoanDecisionResult rejectApplication(Integer applicationId, String identification, String rejectionReason) {
        Integer analystId = resolveAnalyst(identification);
        if (rejectionReason == null || rejectionReason.isBlank() || rejectionReason.length() > 200) {
            throw new LoanDecisionRuleException("El motivo del rechazo es obligatorio y no puede superar los 200 caracteres.");
        }
        LoanApplication application = loadRegisteredApplication(applicationId);
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        application.setAssignedRate(null);
        application.setFinalInstallment(null);
        application.setRejectionReason(rejectionReason.strip());
        application.setStatus(LoanApplicationStatus.REJECTED);
        return saveDecision(application, analystId, identification, businessDate);
    }

    private Integer resolveAnalyst(String identification) {
        if (identification == null || !identification.matches("[0-9]{10}")) {
            throw new CabeceraFaltanteException("La identificación del analista debe contener 10 dígitos.");
        }
        return analystService.findByNationalId(identification).map(AnalystSession::analystId)
                .orElseThrow(() -> new AccesoDenegadoAnalistaException("El analista no está registrado en el sistema."));
    }

    private LoanApplication loadRegisteredApplication(Integer applicationId) {
        if (applicationId == null || applicationId <= 0) {
            throw new SolicitudNoEncontradaException(applicationId);
        }
        LoanApplication application = applicationRepository.findForDecision(applicationId)
                .orElseThrow(() -> new SolicitudNoEncontradaException(applicationId));
        approvalPolicy.ensureRegistered(STATUS.convertToDatabaseColumn(application.getStatus()));
        return application;
    }

    private BigDecimal calculateInstallment(LoanApplication application, BigDecimal annualEffectiveRate, LocalDate businessDate) {
        BigDecimal insuranceFactor = parameterService.getValue("PAR-07", businessDate);
        if (insuranceFactor == null || insuranceFactor.signum() < 0) {
            throw new IllegalStateException("Invalid monthly insurance configuration.");
        }
        BigDecimal insurance = application.getRequestedAmount().multiply(insuranceFactor).setScale(2, RoundingMode.HALF_UP);
        return amortizationService.calculate(application.getRequestedAmount(), annualEffectiveRate,
                        application.getTermMonths(), application.getAmortizationMethod(), businessDate).stream()
                .map(installment -> installment.principal().add(installment.interest()).add(insurance))
                .max(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalStateException("The amortization schedule is empty."))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private LoanDecisionResult saveDecision(LoanApplication application, Integer analystId, String identification,
            LocalDate businessDate) {
        application.setAnalystId(analystId);
        application.setResolutionDate(businessDate);
        application.setLastModifiedBy(identification);
        application.setLastModifiedAt(LocalDateTime.now());
        applicationRepository.saveAndFlush(application);
        return new LoanDecisionResult(application.getId(), STATUS.convertToDatabaseColumn(application.getStatus()),
                application.getAssignedRate(), application.getFinalInstallment(), businessDate,
                application.getRejectionReason(), analystId);
    }
}
