package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizacionService;
import com.grupob.bancobanquito.amortization.CondicionesAmortizacion;
import com.grupob.bancobanquito.amortization.CuotaAmortizacion;
import com.grupob.bancobanquito.amortization.SistemaAmortizacion;
import com.grupob.bancobanquito.origination.LoanDecisionResult;
import com.grupob.bancobanquito.origination.LoanDecisionService;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class LoanDecisionServiceImpl implements LoanDecisionService {

    private static final Logger LOG = LoggerFactory.getLogger(LoanDecisionServiceImpl.class);
    private final SolicitudRepository applicationRepository;
    private final AnalystRepository analystRepository;
    private final LoanApprovalPolicy approvalPolicy;
    private final ParameterService parameterService;
    private final BusinessDateService businessDateService;
    private final AmortizacionService amortizationService;

    LoanDecisionServiceImpl(SolicitudRepository applicationRepository, AnalystRepository analystRepository,
            LoanApprovalPolicy approvalPolicy, ParameterService parameterService,
            BusinessDateService businessDateService, AmortizacionService amortizationService) {
        this.applicationRepository = applicationRepository;
        this.analystRepository = analystRepository;
        this.approvalPolicy = approvalPolicy;
        this.parameterService = parameterService;
        this.businessDateService = businessDateService;
        this.amortizationService = amortizationService;
    }

    @Override
    public LoanDecisionResult approveApplication(Integer applicationId, String identification,
            BigDecimal annualEffectiveRate) {
        Integer analystId = resolveAnalyst(identification);
        Solicitud application = loadRegisteredApplication(applicationId);
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        approvalPolicy.validateAnnualEffectiveRate(annualEffectiveRate,
                parameterService.getValue("PAR-05", businessDate));
        if (annualEffectiveRate.stripTrailingZeros().scale() > 4
                || annualEffectiveRate.compareTo(BigDecimal.TEN) >= 0) {
            throw new LoanDecisionRuleException("La tasa debe tener máximo un entero y cuatro decimales.");
        }
        BigDecimal installment = calculateInstallment(application, annualEffectiveRate, businessDate);
        approvalPolicy.validatePaymentCapacity(application.getIngresosMensuales(), application.getGastosMensuales(),
                installment, parameterService.getValue("PAR-09", businessDate));
        application.setTeaAsignada(annualEffectiveRate.setScale(4, RoundingMode.UNNECESSARY));
        application.setCuotaEvaluacionDefinitiva(installment);
        application.setMotivoRechazo(null);
        application.setEstadoSolicitud(EstadoSolicitud.APROBADA);
        return saveDecision(application, analystId, identification, businessDate);
    }

    @Override
    public LoanDecisionResult rejectApplication(Integer applicationId, String identification, String rejectionReason) {
        Integer analystId = resolveAnalyst(identification);
        if (rejectionReason == null || rejectionReason.isBlank() || rejectionReason.length() > 200) {
            throw new LoanDecisionRuleException("El motivo del rechazo es obligatorio y no puede superar los 200 caracteres.");
        }
        Solicitud application = loadRegisteredApplication(applicationId);
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        application.setTeaAsignada(null);
        application.setCuotaEvaluacionDefinitiva(null);
        application.setMotivoRechazo(rejectionReason.strip());
        application.setEstadoSolicitud(EstadoSolicitud.RECHAZADA);
        return saveDecision(application, analystId, identification, businessDate);
    }

    private Integer resolveAnalyst(String identification) {
        if (identification == null || !identification.matches("[0-9]{10}")) {
            throw new CabeceraFaltanteException("La identificación del analista debe contener 10 dígitos.");
        }
        return analystRepository.findByIdentification(identification).map(Analyst::getId)
                .orElseThrow(() -> new AccesoDenegadoAnalistaException("El analista no está registrado en el sistema."));
    }

    private Solicitud loadRegisteredApplication(Integer applicationId) {
        if (applicationId == null || applicationId <= 0) {
            throw new SolicitudNoEncontradaException(applicationId);
        }
        Solicitud application = applicationRepository.findForDecision(applicationId)
                .orElseThrow(() -> new SolicitudNoEncontradaException(applicationId));
        approvalPolicy.ensureRegistered(application.getEstadoSolicitud().name());
        return application;
    }

    private BigDecimal calculateInstallment(Solicitud application, BigDecimal annualEffectiveRate, LocalDate businessDate) {
        BigDecimal insuranceFactor = parameterService.getValue("PAR-07", businessDate);
        if (insuranceFactor == null || insuranceFactor.signum() < 0) {
            throw new IllegalStateException("Invalid monthly insurance configuration.");
        }
        BigDecimal insurance = application.getMontoSolicitado().multiply(insuranceFactor).setScale(2, RoundingMode.HALF_UP);
        var schedule = amortizationService.generarTabla(new CondicionesAmortizacion(application.getMontoSolicitado(),
                application.getPlazoMeses(), annualEffectiveRate, insurance, businessDate,
                SistemaAmortizacion.valueOf(application.getSistemaAmortizacion())));
        return schedule.cuotas().stream().map(CuotaAmortizacion::totalCuota).max(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalStateException("The amortization schedule is empty."))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private LoanDecisionResult saveDecision(Solicitud application, Integer analystId, String identification,
            LocalDate businessDate) {
        application.setIdAnalista(analystId);
        application.setFechaResolucion(businessDate);
        application.setUsuarioModificacion(identification);
        application.setFechaModificacion(LocalDateTime.now());
        applicationRepository.saveAndFlush(application);
        LOG.info("Loan application {} resolved as {} by analyst {} on business date {}",
                application.getIdSolicitud(), application.getEstadoSolicitud(), analystId, businessDate);
        return new LoanDecisionResult(application.getIdSolicitud(), application.getEstadoSolicitud().name(),
                application.getTeaAsignada(), application.getCuotaEvaluacionDefinitiva(), businessDate,
                application.getMotivoRechazo(), analystId);
    }
}
