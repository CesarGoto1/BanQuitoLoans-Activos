package com.grupob.bancobanquito.otorgamiento.internal;

import com.grupob.bancobanquito.otorgamiento.SolicitudBandejaResponse;
import com.grupob.bancobanquito.otorgamiento.SolicitudDetalleResponse;
import com.grupob.bancobanquito.otorgamiento.SolicitudService;
import com.grupob.bancobanquito.parameters.ParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SolicitudServiceImpl implements SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudServiceImpl.class);

    private final SolicitudRepository solicitudRepository;
    private final ParameterService parameterService;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                ParameterService parameterService) {
        this.solicitudRepository = solicitudRepository;
        this.parameterService = parameterService;
    }

    @Override
    public List<SolicitudBandejaResponse> listarBandejaAnalista(String estado) {
        EstadoSolicitud estadoFiltro = parsearEstado(estado);
        log.info("Consultando bandeja de analista para estado: {}", estadoFiltro);

        List<Solicitud> solicitudes = this.solicitudRepository
                .findByEstadoSolicitudOrderByFechaHoraRegistroAsc(estadoFiltro);

        return solicitudes.stream()
                .map(this::mapearABandejaResponse)
                .toList();
    }

    @Override
    public SolicitudDetalleResponse obtenerDetalleParaEvaluacion(Integer idSolicitud) {
        log.info("Consultando detalle para evaluación de solicitud ID: {}", idSolicitud);

        Solicitud solicitud = this.solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException(idSolicitud));

        return mapearADetalleResponse(solicitud);
    }

    private EstadoSolicitud parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return EstadoSolicitud.REGISTRADA;
        }
        try {
            return EstadoSolicitud.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Estado desconocido '{}', aplicando filtro por defecto REGISTRADA", estado);
            return EstadoSolicitud.REGISTRADA;
        }
    }

    private SolicitudBandejaResponse mapearABandejaResponse(Solicitud s) {
        return new SolicitudBandejaResponse(
                s.getIdSolicitud(),
                s.getIdCliente(),
                s.getMontoSolicitado(),
                s.getPlazoMeses(),
                s.getSistemaAmortizacion(),
                s.getDestinoCredito(),
                s.getCuotaEvaluacionReferencial(),
                s.getFechaSolicitud(),
                s.getFechaHoraRegistro(),
                s.getEstadoSolicitud().name()
        );
    }

    private SolicitudDetalleResponse mapearADetalleResponse(Solicitud s) {
        BigDecimal ingresos = s.getIngresosMensuales().setScale(2, RoundingMode.HALF_UP);
        BigDecimal gastos = s.getGastosMensuales().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remanente = ingresos.subtract(gastos).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cuotaReferencial = s.getCuotaEvaluacionReferencial().setScale(2, RoundingMode.HALF_UP);

        BigDecimal factorPar09 = this.parameterService.getValue("PAR-09");
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

            capacidadValida = porcentajeImpacto.compareTo(limiteImpactoMaximo) <= 0;

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
                s.getIdSolicitud(),
                s.getIdCliente(),
                s.getEstadoSolicitud().name(),
                s.getFechaSolicitud(),
                s.getFechaHoraRegistro(),
                s.getMontoSolicitado(),
                s.getPlazoMeses(),
                s.getSistemaAmortizacion(),
                s.getDestinoCredito(),
                cuotaReferencial,
                ingresos,
                gastos,
                remanente,
                porcentajeImpacto,
                limiteImpactoMaximo,
                cuotaMaximaAdmisible,
                capacidadValida,
                mensajeCapacidad,
                s.getTeaAsignada(),
                s.getCuotaEvaluacionDefinitiva(),
                s.getFechaResolucion(),
                s.getMotivoRechazo(),
                s.getIdAnalista()
        );
    }
}
