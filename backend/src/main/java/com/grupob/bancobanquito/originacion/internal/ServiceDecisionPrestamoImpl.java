package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.originacion.ResultadoDecisionPrestamo;
import com.grupob.bancobanquito.originacion.ServiceDecisionPrestamo;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;

@Service
@Transactional
class ServiceDecisionPrestamoImpl implements ServiceDecisionPrestamo {

    private final SolicitudRepository applicationRepository;
    private final ServiceAnalista analystService;
    private final PoliticaAprobacionPrestamo politicaAprobacion;
    private final ServiceParametro servicioParametro;
    private final ServiceFechaNegocio servicioFechaNegocio;
    private final ServiceAmortizacion servicioAmortizacion;

    ServiceDecisionPrestamoImpl(SolicitudRepository applicationRepository, ServiceAnalista analystService,
            PoliticaAprobacionPrestamo politicaAprobacion, ServiceParametro servicioParametro,
            ServiceFechaNegocio servicioFechaNegocio, ServiceAmortizacion servicioAmortizacion) {
        this.applicationRepository = applicationRepository;
        this.analystService = analystService;
        this.politicaAprobacion = politicaAprobacion;
        this.servicioParametro = servicioParametro;
        this.servicioFechaNegocio = servicioFechaNegocio;
        this.servicioAmortizacion = servicioAmortizacion;
    }

    @Override
    public ResultadoDecisionPrestamo approveApplication(Integer idSolicitud, String identification,
            BigDecimal teaAsignada) {
        Integer idAnalista = resolveAnalyst(identification);
        Solicitud application = loadRegisteredApplication(idSolicitud);
        LocalDate fechaNegocio = servicioFechaNegocio.getCurrentBusinessDate();
        politicaAprobacion.validateAnnualEffectiveRate(teaAsignada,
                servicioParametro.getValue("PAR-05", fechaNegocio));
        if (teaAsignada.stripTrailingZeros().scale() > 4
                || teaAsignada.compareTo(BigDecimal.TEN) >= 0) {
            throw new ExcepcionReglaDecisionPrestamo("La tasa debe tener máximo un entero y cuatro decimales.");
        }
        BigDecimal installment = calculateInstallment(application, teaAsignada, fechaNegocio);
        politicaAprobacion.validatePaymentCapacity(application.getMonthlyIncome(), application.getMonthlyExpenses(),
                installment, servicioParametro.getValue("PAR-09", fechaNegocio));
        application.setAssignedRate(teaAsignada.setScale(4, RoundingMode.UNNECESSARY));
        application.setFinalInstallment(installment);
        application.setRejectionReason(null);
        application.setStatus(EstadoSolicitud.APROBADA);
        return saveDecision(application, idAnalista, identification, fechaNegocio);
    }

    @Override
    public ResultadoDecisionPrestamo rejectApplication(Integer idSolicitud, String identification, String motivoRechazo) {
        Integer idAnalista = resolveAnalyst(identification);
        if (motivoRechazo == null || motivoRechazo.isBlank() || motivoRechazo.length() > 200) {
            throw new ExcepcionReglaDecisionPrestamo("El motivo del rechazo es obligatorio y no puede superar los 200 caracteres.");
        }
        Solicitud application = loadRegisteredApplication(idSolicitud);
        LocalDate fechaNegocio = servicioFechaNegocio.getCurrentBusinessDate();
        application.setAssignedRate(null);
        application.setFinalInstallment(null);
        application.setRejectionReason(motivoRechazo.strip());
        application.setStatus(EstadoSolicitud.RECHAZADA);
        return saveDecision(application, idAnalista, identification, fechaNegocio);
    }

    private Integer resolveAnalyst(String identification) {
        if (identification == null || !identification.matches("[0-9]{10}")) {
            throw new CabeceraFaltanteException("La identificación del analista debe contener 10 dígitos.");
        }
        return analystService.findByNationalId(identification).map(SesionAnalista::idAnalista)
                .orElseThrow(() -> new AccesoDenegadoAnalistaException("El analista no está registrado en el sistema."));
    }

    private Solicitud loadRegisteredApplication(Integer idSolicitud) {
        if (idSolicitud == null || idSolicitud <= 0) {
            throw new SolicitudNoEncontradaException(idSolicitud);
        }
        Solicitud application = applicationRepository.findForDecision(idSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException(idSolicitud));
        politicaAprobacion.ensureRegistered(application.getStatus().name());
        return application;
    }

    private BigDecimal calculateInstallment(Solicitud application, BigDecimal teaAsignada, LocalDate fechaNegocio) {
        BigDecimal insuranceFactor = servicioParametro.getValue("PAR-07", fechaNegocio);
        if (insuranceFactor == null || insuranceFactor.signum() < 0) {
            throw new IllegalStateException("Invalid monthly insurance configuration.");
        }
        BigDecimal seguro = application.getRequestedAmount().multiply(insuranceFactor).setScale(2, RoundingMode.HALF_UP);
        return servicioAmortizacion.calculate(application.getRequestedAmount(), teaAsignada,
                        application.getTermMonths(), application.getAmortizationMethod(), fechaNegocio).stream()
                .map(installment -> installment.capital().add(installment.interes()).add(seguro))
                .max(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalStateException("The amortization schedule is empty."))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ResultadoDecisionPrestamo saveDecision(Solicitud application, Integer idAnalista, String identification,
            LocalDate fechaNegocio) {
        application.setAnalystId(idAnalista);
        application.setResolutionDate(fechaNegocio);
        application.setLastModifiedBy(identification);
        application.setLastModifiedAt(LocalDateTime.now());
        applicationRepository.saveAndFlush(application);
        return new ResultadoDecisionPrestamo(application.getId(), application.getStatus().name(),
                application.getAssignedRate(), application.getFinalInstallment(), fechaNegocio,
                application.getRejectionReason(), idAnalista);
    }
}
