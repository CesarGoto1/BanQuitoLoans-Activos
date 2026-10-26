package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.originacion.SolicitudBandejaResponse;
import com.grupob.bancobanquito.originacion.SolicitudDetalleResponse;
import com.grupob.bancobanquito.originacion.ServiceBandejaSolicitud;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServiceBandejaSolicitudImpl implements ServiceBandejaSolicitud {

    private static final ConversorEstadoSolicitud STATUS = new ConversorEstadoSolicitud();
    private static final ConversorMetodoAmortizacion METHOD = new ConversorMetodoAmortizacion();
    private static final ConversorDestinoCredito PURPOSE = new ConversorDestinoCredito();

    private final SolicitudRepository solicitudRepository;
    private final ServiceParametro servicioParametro;

    public ServiceBandejaSolicitudImpl(SolicitudRepository solicitudRepository,
                                ServiceParametro servicioParametro) {
        this.solicitudRepository = solicitudRepository;
        this.servicioParametro = servicioParametro;
    }

    @Override
    public List<SolicitudBandejaResponse> listarBandejaAnalista(String estado) {
        return this.solicitudRepository.findByStatusOrderByFechaHoraRegistroAsc(parsearEstado(estado)).stream()
                .map(this::mapearABandejaResponse)
                .toList();
    }

    @Override
    public SolicitudDetalleResponse obtenerDetalleParaEvaluacion(Integer idSolicitud) {
        Solicitud solicitud = this.solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException(idSolicitud));
        return mapearADetalleResponse(solicitud);
    }

    private EstadoSolicitud parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return EstadoSolicitud.REGISTERED;
        }
        try {
            return STATUS.convertToEntityAttribute(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EstadoSolicitud.REGISTERED;
        }
    }

    private SolicitudBandejaResponse mapearABandejaResponse(Solicitud s) {
        return new SolicitudBandejaResponse(
                s.getId(),
                s.getCustomerId(),
                s.getRequestedAmount(),
                s.getTermMonths(),
                METHOD.convertToDatabaseColumn(s.getAmortizationMethod()),
                PURPOSE.convertToDatabaseColumn(s.getPurpose()),
                s.getReferenceInstallment(),
                s.getApplicationDate(),
                s.getRegisteredAt(),
                STATUS.convertToDatabaseColumn(s.getStatus())
        );
    }

    private SolicitudDetalleResponse mapearADetalleResponse(Solicitud s) {
        BigDecimal ingresos = s.getMonthlyIncome().setScale(2, RoundingMode.HALF_UP);
        BigDecimal gastos = s.getMonthlyExpenses().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remanente = ingresos.subtract(gastos).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cuotaReferencial = s.getReferenceInstallment().setScale(2, RoundingMode.HALF_UP);

        BigDecimal factorPar09 = this.servicioParametro.getValue("PAR-09");
        BigDecimal limiteImpactoMaximo = factorPar09.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal porcentajeImpacto;
        BigDecimal cuotaMaximaAdmisible;
        boolean capacidadValida;
        String mensajeCapacidad;

        if (remanente.compareTo(BigDecimal.ZERO) <= 0) {
            porcentajeImpacto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            cuotaMaximaAdmisible = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            capacidadValida = false;
            mensajeCapacidad = "El solicitante no dispone de remanente mensual positivo (RN-10).";
        } else {
            cuotaMaximaAdmisible = remanente.multiply(factorPar09).setScale(2, RoundingMode.HALF_UP);
            porcentajeImpacto = cuotaReferencial
                    .divide(remanente, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            capacidadValida = cuotaReferencial.compareTo(cuotaMaximaAdmisible) <= 0;

            if (capacidadValida) {
                mensajeCapacidad = "La cuota referencial representa el " + porcentajeImpacto
                        + "% del remanente (dentro del límite regulatorio del " + limiteImpactoMaximo + "%).";
            } else {
                mensajeCapacidad = "ALERTA: La cuota referencial (" + porcentajeImpacto
                        + "%) supera el límite regulatorio del " + limiteImpactoMaximo
                        + "% del remanente mensual (RN-09). Cuota máxima admisible: $" + cuotaMaximaAdmisible + ".";
            }
        }

        return new SolicitudDetalleResponse(
                s.getId(),
                s.getCustomerId(),
                STATUS.convertToDatabaseColumn(s.getStatus()),
                s.getApplicationDate(),
                s.getRegisteredAt(),
                s.getRequestedAmount(),
                s.getTermMonths(),
                METHOD.convertToDatabaseColumn(s.getAmortizationMethod()),
                PURPOSE.convertToDatabaseColumn(s.getPurpose()),
                cuotaReferencial,
                ingresos,
                gastos,
                remanente,
                porcentajeImpacto,
                limiteImpactoMaximo,
                cuotaMaximaAdmisible,
                capacidadValida,
                mensajeCapacidad,
                s.getAssignedRate(),
                s.getFinalInstallment(),
                s.getResolutionDate(),
                s.getRejectionReason(),
                s.getAnalystId()
        );
    }
}
