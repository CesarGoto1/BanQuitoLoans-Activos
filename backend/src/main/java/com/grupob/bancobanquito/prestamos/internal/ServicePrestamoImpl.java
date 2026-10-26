package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.amortizacion.CuotaCalculada;
import com.grupob.bancobanquito.pasivos.Cuenta;
import com.grupob.bancobanquito.pasivos.Cliente;
import com.grupob.bancobanquito.pasivos.InstruccionDesembolso;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import com.grupob.bancobanquito.prestamos.VistaPreviaDesembolso;
import com.grupob.bancobanquito.prestamos.ResultadoDesembolso;
import com.grupob.bancobanquito.prestamos.ServicePrestamo;
import com.grupob.bancobanquito.prestamos.DesembolsoPendiente;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.originacion.SolicitudAprobada;
import com.grupob.bancobanquito.originacion.ServiceSolicitudes;
import com.grupob.bancobanquito.originacion.SolicitudDesembolsoPendiente;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
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
class ServicePrestamoImpl implements ServicePrestamo {

    private static final String INSURANCE_RATE = "PAR-07";
    private static final String SOLCA_RATE = "PAR-08";
    private static final String AUDIT_USER = "system";
    private static final DateTimeFormatter LOAN_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PrestamoRepository loanRepository;
    private final CuotaRepository installmentRepository;
    private final ServiceSolicitudes servicioSolicitudes;
    private final ServiceAmortizacion servicioAmortizacion;
    private final ServicePasivos servicioPasivos;
    private final ServiceParametro servicioParametro;
    private final ServiceFechaNegocio servicioFechaNegocio;

    ServicePrestamoImpl(PrestamoRepository loanRepository, CuotaRepository installmentRepository,
                    ServiceSolicitudes servicioSolicitudes, ServiceAmortizacion servicioAmortizacion,
                    ServicePasivos servicioPasivos, ServiceParametro servicioParametro,
                    ServiceFechaNegocio servicioFechaNegocio) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.servicioSolicitudes = servicioSolicitudes;
        this.servicioAmortizacion = servicioAmortizacion;
        this.servicioPasivos = servicioPasivos;
        this.servicioParametro = servicioParametro;
        this.servicioFechaNegocio = servicioFechaNegocio;
    }

    @Override
    @Transactional(readOnly = true)
    public VistaPreviaDesembolso previewDisbursement(Integer idSolicitud) {
        SolicitudAprobada application = pendingApplication(idSolicitud);
        Cliente customer = customer(application.idCliente());
        BigDecimal contribucionSolca = contribucionSolca(application.montoAprobado());

        return new VistaPreviaDesembolso(application.idSolicitud(), customer.cedula(),
                customer.nombres() + " " + customer.apellidos(), application.montoAprobado(),
                application.teaAsignada(), application.plazoMeses(), application.metodoAmortizacion(),
                seguroMensual(application.montoAprobado()), contribucionSolca,
                application.montoAprobado().subtract(contribucionSolca),
                servicioFechaNegocio.getCurrentBusinessDate(),
                servicioPasivos.findActiveAccounts(application.idCliente()));
    }

    @Override
    @Transactional
    public ResultadoDesembolso disburse(Integer idSolicitud, String cuentaDestino) {
        SolicitudAprobada application = pendingApplication(idSolicitud);
        Cliente customer = customer(application.idCliente());

        List<Cuenta> cuentas = servicioPasivos.findActiveAccounts(application.idCliente());
        if (cuentas.stream().noneMatch(account -> account.numeroCuenta().equals(cuentaDestino))) {
            throw new ExcepcionPrestamo(HttpStatus.BAD_REQUEST,
                    "La cuenta " + cuentaDestino + " no es una cuenta activa del cliente.");
        }

        LocalDate fechaDesembolso = servicioFechaNegocio.getCurrentBusinessDate();
        BigDecimal montoAprobado = application.montoAprobado();
        BigDecimal seguroMensual = seguroMensual(montoAprobado);
        BigDecimal contribucionSolca = contribucionSolca(montoAprobado);

        Prestamo loan = new Prestamo();
        loan.setApplicationId(application.idSolicitud());
        loan.setCustomerId(application.idCliente());
        loan.setCustomerNationalId(customer.cedula());
        loan.setAmortizationMethod(application.metodoAmortizacion());
        loan.setLoanNumber(numeroOperacion(fechaDesembolso, idSolicitud));
        loan.setApprovedAmount(montoAprobado);
        loan.setEffectiveAnnualRate(application.teaAsignada());
        loan.setTermMonths(application.plazoMeses());
        loan.setMonthlyInsurance(seguroMensual);
        loan.setSolcaContribution(contribucionSolca);
        loan.setNetAmount(montoAprobado.subtract(contribucionSolca));
        loan.setDestinationAccount(cuentaDestino);
        loan.setDisbursementDate(fechaDesembolso);
        loan.setDisbursementInstructionStatus(EstadoInstruccionDesembolso.PENDIENTE);
        loan.setPortfolioStatus(EstadoCartera.POR_VENCER);
        loan.setPrincipalBalance(montoAprobado);
        loan.setDaysPastDue(0);
        loan.setCreatedBy(AUDIT_USER);
        loan.setCreatedAt(LocalDateTime.now());
        loanRepository.save(loan);

        List<Cuota> cuotas = new ArrayList<>();
        for (CuotaCalculada calculated : servicioAmortizacion.calculate(montoAprobado,
                application.teaAsignada(), application.plazoMeses(), application.metodoAmortizacion(),
                fechaDesembolso)) {
            cuotas.add(newInstallment(loan, calculated, seguroMensual));
        }
        installmentRepository.saveAll(cuotas);
        loanRepository.flush();

        try {
            servicioPasivos.creditDisbursement(new InstruccionDesembolso(loan.getLoanNumber(),
                    customer.cedula(), cuentaDestino, montoAprobado, contribucionSolca,
                    loan.getNetAmount(), fechaDesembolso));
        } catch (RuntimeException e) {
            throw new ExcepcionPrestamo(HttpStatus.BAD_GATEWAY,
                    "No se pudo acreditar en el Core de Pasivos; el desembolso no se realizó. Intenta de nuevo.");
        }
        loan.setDisbursementInstructionStatus(EstadoInstruccionDesembolso.ENTREGADA);
        loan.setLastModifiedBy(AUDIT_USER);
        loan.setLastModifiedAt(LocalDateTime.now());

        return result(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultadoDesembolso> findDisbursement(Integer idSolicitud) {
        return loanRepository.findByIdSolicitud(idSolicitud).map(this::result);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DesembolsoPendiente> findPendingDisbursements(Pageable paginacion) {
        return servicioSolicitudes.findPendingDisbursement(paginacion).map(this::pendingDisbursement);
    }

    private DesembolsoPendiente pendingDisbursement(SolicitudDesembolsoPendiente application) {
        Optional<Cliente> customer = servicioPasivos.findCustomer(application.idCliente());
        return new DesembolsoPendiente(application.idSolicitud(), application.idCliente(),
                customer.map(Cliente::cedula).orElse(null),
                customer.map(found -> found.nombres() + " " + found.apellidos()).orElse(null),
                application.montoAprobado(), application.teaAsignada(), application.plazoMeses(),
                application.metodoAmortizacion(), application.fechaResolucion());
    }

    private SolicitudAprobada pendingApplication(Integer idSolicitud) {
        SolicitudAprobada application = servicioSolicitudes.findApproved(idSolicitud)
                .orElseThrow(() -> new ExcepcionPrestamo(HttpStatus.NOT_FOUND,
                        "No existe una solicitud aprobada con el número " + idSolicitud + "."));
        if (loanRepository.existsByIdSolicitud(idSolicitud)) {
            throw new ExcepcionPrestamo(HttpStatus.CONFLICT,
                    "La solicitud " + idSolicitud + " ya fue desembolsada.");
        }
        return application;
    }

    private Cliente customer(Integer idCliente) {
        return servicioPasivos.findCustomer(idCliente)
                .orElseThrow(() -> new ExcepcionPrestamo(HttpStatus.NOT_FOUND,
                        "El cliente " + idCliente + " no existe en el Core de Pasivos."));
    }

    private BigDecimal seguroMensual(BigDecimal montoAprobado) {
        return montoAprobado.multiply(servicioParametro.getValue(INSURANCE_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal contribucionSolca(BigDecimal montoAprobado) {
        return montoAprobado.multiply(servicioParametro.getValue(SOLCA_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    private String numeroOperacion(LocalDate fechaDesembolso, Integer idSolicitud) {
        return "OP-" + fechaDesembolso.format(LOAN_NUMBER_DATE) + "-" + String.format("%06d", idSolicitud);
    }

    private Cuota newInstallment(Prestamo loan, CuotaCalculada calculated, BigDecimal seguroMensual) {
        Cuota installment = new Cuota();
        installment.setLoan(loan);
        installment.setInstallmentNumber(calculated.numeroCuota());
        installment.setDueDate(calculated.fechaVencimiento());
        installment.setPrincipalAmount(calculated.capital());
        installment.setInterestAmount(calculated.interes());
        installment.setInsuranceAmount(seguroMensual);
        installment.setRemainingPrincipalBalance(calculated.remainingPrincipalBalance());
        installment.setInsurancePaid(BigDecimal.ZERO);
        installment.setDefaultInterestPaid(BigDecimal.ZERO);
        installment.setInterestPaid(BigDecimal.ZERO);
        installment.setPrincipalPaid(BigDecimal.ZERO);
        installment.setAccruedDefaultInterest(BigDecimal.ZERO);
        installment.setStatus(EstadoCuota.PENDIENTE);
        installment.setCreatedBy(AUDIT_USER);
        installment.setCreatedAt(loan.getCreatedAt());
        return installment;
    }

    private ResultadoDesembolso result(Prestamo loan) {
        return new ResultadoDesembolso(loan.getApplicationId(), loan.getLoanNumber(), loan.getNetAmount(),
                loan.getDestinationAccount(), loan.getDisbursementDate());
    }
}
