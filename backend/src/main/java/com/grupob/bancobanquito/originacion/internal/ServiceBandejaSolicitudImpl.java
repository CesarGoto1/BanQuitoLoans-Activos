package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.originacion.SolicitudBandejaResponse;
import com.grupob.bancobanquito.originacion.SolicitudDetalleResponse;
import com.grupob.bancobanquito.originacion.ServiceBandejaSolicitud;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.pasivos.Cliente;
import com.grupob.bancobanquito.pasivos.PasivosNoDisponibleException;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ServiceBandejaSolicitudImpl implements ServiceBandejaSolicitud {

    private final SolicitudRepository solicitudRepository;
    private final ServiceParametro servicioParametro;
    private final ServicePasivos servicioPasivos;

    public ServiceBandejaSolicitudImpl(SolicitudRepository solicitudRepository,
                                ServiceParametro servicioParametro,
                                ServicePasivos servicioPasivos) {
        this.solicitudRepository = solicitudRepository;
        this.servicioParametro = servicioParametro;
        this.servicioPasivos = servicioPasivos;
    }

    @Override
    public List<SolicitudBandejaResponse> listarBandejaAnalista(String estado) {
        Map<Integer, Optional<Cliente>> clientes = new HashMap<>();
        return this.solicitudRepository.findByStatusOrderByFechaHoraRegistroAsc(parsearEstado(estado)).stream()
                .map(solicitud -> mapearABandejaResponse(solicitud,
                        clientes.computeIfAbsent(solicitud.getCustomerId(), this::buscarCliente)))
                .toList();
    }

    @Override
    public SolicitudDetalleResponse obtenerDetalleParaEvaluacion(Integer idSolicitud) {
        Solicitud solicitud = this.solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new SolicitudNoEncontradaException(idSolicitud));
        return mapearADetalleResponse(solicitud, buscarCliente(solicitud.getCustomerId()));
    }

    private Optional<Cliente> buscarCliente(Integer idCliente) {
        try {
            return this.servicioPasivos.findCustomer(idCliente);
        } catch (PasivosNoDisponibleException e) {
            return Optional.empty();
        }
    }

    private String nombreCompleto(Cliente cliente) {
        return cliente.nombres() + " " + cliente.apellidos();
    }

    private EstadoSolicitud parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return EstadoSolicitud.REGISTRADA;
        }
        try {
            return EstadoSolicitud.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EstadoSolicitud.REGISTRADA;
        }
    }

    private SolicitudBandejaResponse mapearABandejaResponse(Solicitud s, Optional<Cliente> cliente) {
        return new SolicitudBandejaResponse(
                s.getId(),
                s.getCustomerId(),
                cliente.map(Cliente::cedula).orElse(null),
                cliente.map(this::nombreCompleto).orElse(null),
                s.getRequestedAmount(),
                s.getTermMonths(),
                s.getAmortizationMethod().name(),
                s.getPurpose().name(),
                s.getReferenceInstallment(),
                s.getApplicationDate(),
                s.getRegisteredAt(),
                s.getStatus().name()
        );
    }

    private SolicitudDetalleResponse mapearADetalleResponse(Solicitud s, Optional<Cliente> cliente) {
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
            mensajeCapacidad = "El solicitante no dispone de remanente mensual positivo.";
        } else {
            cuotaMaximaAdmisible = remanente.multiply(factorPar09).setScale(2, RoundingMode.HALF_UP);
            porcentajeImpacto = cuotaReferencial
                    .divide(remanente, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            capacidadValida = cuotaReferencial.compareTo(cuotaMaximaAdmisible) <= 0;

            if (capacidadValida) {
                mensajeCapacidad = "La cuota referencial representa el " + decimal(porcentajeImpacto)
                        + " % del remanente mensual, dentro del límite regulatorio del " + decimal(limiteImpactoMaximo) + " %.";
            } else {
                mensajeCapacidad = "La cuota referencial (" + decimal(porcentajeImpacto)
                        + " %) supera el límite regulatorio del " + decimal(limiteImpactoMaximo)
                        + " % del remanente mensual. Cuota máxima admisible: USD " + decimal(cuotaMaximaAdmisible) + ".";
            }
        }

        return new SolicitudDetalleResponse(
                s.getId(),
                s.getCustomerId(),
                cliente.map(Cliente::cedula).orElse(null),
                cliente.map(this::nombreCompleto).orElse(null),
                s.getStatus().name(),
                s.getApplicationDate(),
                s.getRegisteredAt(),
                s.getRequestedAmount(),
                s.getTermMonths(),
                s.getAmortizationMethod().name(),
                s.getPurpose().name(),
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

    private String decimal(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
