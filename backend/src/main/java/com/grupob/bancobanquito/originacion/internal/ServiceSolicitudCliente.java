package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
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
class ServiceSolicitudCliente {

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

    private final SolicitudRepository loanApplicationRepository;
    private final PrestamoClienteRepository prestamoClienteRepositorio;
    private final ServicePasivos servicioPasivos;
    private final ServiceParametro servicioParametro;
    private final ServiceFechaNegocio servicioFechaNegocio;
    private final ServiceAmortizacion servicioAmortizacion;

    ServiceSolicitudCliente(SolicitudRepository loanApplicationRepository,
                               PrestamoClienteRepository prestamoClienteRepositorio, ServicePasivos servicioPasivos,
                               ServiceParametro servicioParametro, ServiceFechaNegocio servicioFechaNegocio,
                               ServiceAmortizacion servicioAmortizacion) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.prestamoClienteRepositorio = prestamoClienteRepositorio;
        this.servicioPasivos = servicioPasivos;
        this.servicioParametro = servicioParametro;
        this.servicioFechaNegocio = servicioFechaNegocio;
        this.servicioAmortizacion = servicioAmortizacion;
    }

    @Transactional
    Solicitud register(Integer idCliente, PeticionSolicitud request) {
        LocalDate fechaNegocio = servicioFechaNegocio.getCurrentBusinessDate();
        validateRanges(request.monto(), request.plazoMeses(), fechaNegocio);
        if (servicioPasivos.findCustomer(idCliente).isEmpty()) {
            throw new ExcepcionOriginacion(HttpStatus.NOT_FOUND, "No existe el cliente " + idCliente + ".");
        }
        if (prestamoClienteRepositorio.existsByIdClienteAndDaysPastDueGreaterThan(idCliente, 0)) {
            throw new ExcepcionOriginacion(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Tienes una operación con días de mora, por eso no puedes registrar una nueva solicitud.");
        }
        if (loanApplicationRepository.existsByIdClienteAndStatus(idCliente, EstadoSolicitud.REGISTRADA)) {
            throw new ExcepcionOriginacion(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Ya tienes una solicitud pendiente de revisión.");
        }

        BigDecimal installment = simulate(request.monto(), request.plazoMeses(), request.metodoAmortizacion(),
                fechaNegocio).highestInstallment();
        BigDecimal remaining = request.ingresosMensuales().subtract(request.gastosMensuales());
        BigDecimal limit = servicioParametro.getValue(INSTALLMENT_LIMIT, fechaNegocio);

        Solicitud application = new Solicitud();
        application.setCustomerId(idCliente);
        application.setAmortizationMethod(request.metodoAmortizacion());
        application.setRequestedAmount(request.monto());
        application.setTermMonths(request.plazoMeses());
        application.setPurpose(request.destinoCredito());
        application.setMonthlyIncome(request.ingresosMensuales());
        application.setMonthlyExpenses(request.gastosMensuales());
        application.setApplicationDate(fechaNegocio);
        application.setRegisteredAt(LocalDateTime.now());
        application.setReferenceInstallment(installment);
        application.setStatus(EstadoSolicitud.REGISTRADA);
        application.setCreatedBy(AUDIT_USER);
        application.setCreatedAt(LocalDateTime.now());

        if (remaining.signum() <= 0) {
            reject(application, fechaNegocio, "Tu remanente mensual (ingresos menos gastos) es cero o negativo.");
        } else {
            BigDecimal maxInstallment = remaining.multiply(limit).setScale(2, RoundingMode.DOWN);
            if (installment.compareTo(maxInstallment) > 0) {
                reject(application, fechaNegocio, "La cuota proyectada de USD " + money(installment)
                        + " supera la cuota máxima admisible de USD " + money(maxInstallment) + " ("
                        + limit.movePointRight(2).stripTrailingZeros().toPlainString()
                        + " % de tu remanente mensual).");
            }
        }
        return loanApplicationRepository.save(application);
    }

    Integer customerIdOf(String cedula) {
        return servicioPasivos.findCustomerByNationalId(cedula)
                .map(customer -> customer.idCliente())
                .orElseThrow(() -> new ExcepcionOriginacion(HttpStatus.NOT_FOUND,
                        "No existe un cliente con la cédula " + cedula + "."));
    }

    @Transactional(readOnly = true)
    SimulacionPrestamo simulate(PeticionSimulacionPrestamo request) {
        LocalDate fechaNegocio = servicioFechaNegocio.getCurrentBusinessDate();
        validateRanges(request.monto(), request.plazoMeses(), fechaNegocio);
        return simulate(request.monto(), request.plazoMeses(), request.metodoAmortizacion(), fechaNegocio);
    }

    @Transactional(readOnly = true)
    Page<Solicitud> findByCustomer(Integer idCliente, EstadoSolicitud status, Pageable paginacion) {
        return status == null
                ? loanApplicationRepository.findByIdCliente(idCliente, paginacion)
                : loanApplicationRepository.findByIdClienteAndStatus(idCliente, status, paginacion);
    }

    @Transactional(readOnly = true)
    LimitesCredito findCreditLimits() {
        return creditLimits(servicioFechaNegocio.getCurrentBusinessDate());
    }

    private LimitesCredito creditLimits(LocalDate fechaNegocio) {
        return new LimitesCredito(servicioParametro.getValue(MIN_AMOUNT, fechaNegocio).setScale(2, RoundingMode.HALF_UP),
                servicioParametro.getValue(MAX_AMOUNT, fechaNegocio).setScale(2, RoundingMode.HALF_UP),
                servicioParametro.getValue(MIN_TERM, fechaNegocio).intValue(),
                servicioParametro.getValue(MAX_TERM, fechaNegocio).intValue());
    }

    private void validateRanges(BigDecimal monto, Integer plazoMeses, LocalDate fechaNegocio) {
        LimitesCredito limits = creditLimits(fechaNegocio);
        if (monto.compareTo(limits.montoMinimo()) < 0 || monto.compareTo(limits.montoMaximo()) > 0) {
            throw new ExcepcionOriginacion(HttpStatus.UNPROCESSABLE_CONTENT, "El monto debe estar entre USD "
                    + money(limits.montoMinimo()) + " y USD " + money(limits.montoMaximo()) + ".");
        }
        if (plazoMeses < limits.plazoMinimoMeses() || plazoMeses > limits.plazoMaximoMeses()) {
            throw new ExcepcionOriginacion(HttpStatus.UNPROCESSABLE_CONTENT, "El plazo debe estar entre "
                    + limits.plazoMinimoMeses() + " y " + limits.plazoMaximoMeses() + " meses.");
        }
    }

    private SimulacionPrestamo simulate(BigDecimal monto, Integer plazoMeses, MetodoAmortizacion metodo,
                                    LocalDate fechaNegocio) {
        BigDecimal rate = servicioParametro.getValue(REFERENCE_RATE, fechaNegocio);
        BigDecimal seguro = monto.multiply(servicioParametro.getValue(INSURANCE_RATE, fechaNegocio))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal solca = monto.multiply(servicioParametro.getValue(SOLCA_RATE, fechaNegocio))
                .setScale(2, RoundingMode.HALF_UP);
        List<CuotaSimulada> cuotas = servicioAmortizacion
                .calculate(monto, rate, plazoMeses, metodo, fechaNegocio).stream()
                .map(installment -> new CuotaSimulada(installment.numeroCuota(), installment.fechaVencimiento(),
                        installment.capital(), installment.interes(), seguro,
                        installment.capital().add(installment.interes()).add(seguro),
                        installment.remainingPrincipalBalance()))
                .toList();
        BigDecimal interesTotal = cuotas.stream().map(CuotaSimulada::interes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal seguroTotal = seguro.multiply(BigDecimal.valueOf(plazoMeses));
        return new SimulacionPrestamo(monto, plazoMeses, metodo, rate, nominalRate(rate), seguro, solca,
                monto.subtract(solca), cuotas.getFirst().total(), cuotas.getLast().total(),
                interesTotal, seguroTotal, interesTotal.add(seguroTotal).add(solca), cuotas);
    }

    private BigDecimal nominalRate(BigDecimal teaAsignada) {
        return servicioAmortizacion.monthlyRate(teaAsignada).multiply(BigDecimal.valueOf(12))
                .setScale(6, RoundingMode.HALF_UP);
    }

    private void reject(Solicitud application, LocalDate fechaNegocio, String reason) {
        application.setStatus(EstadoSolicitud.RECHAZADA);
        application.setResolutionDate(fechaNegocio);
        application.setRejectionReason(reason);
    }

    private String money(BigDecimal value) {
        return String.format(ECUADOR, "%,.2f", value);
    }
}
