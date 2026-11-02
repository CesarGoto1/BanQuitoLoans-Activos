package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;
import com.grupob.bancobanquito.amortizacion.CuotaCalculada;
import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import com.grupob.bancobanquito.amortizacion.RecalculoTabla;
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.CuadroAmortizacion;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ResultadoAbono;
import com.grupob.bancobanquito.prestamos.ResumenCuota;
import com.grupob.bancobanquito.prestamos.ServiceAbonoExtraordinario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceAbonoExtraordinarioImpl implements ServiceAbonoExtraordinario {

    private static final String BASE_DIAS = "PAR-11";
    private static final String ABONO_MINIMO = "PAR-12";

    private final PrestamoRepository prestamoRepository;
    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final ServiceAmortizacion servicioAmortizacion;
    private final ServiceFechaNegocio servicioFechaNegocio;
    private final ServiceParametro servicioParametro;
    private final ServiceAnalista servicioAnalista;
    private final ServiceContabilidad servicioContabilidad;

    ServiceAbonoExtraordinarioImpl(PrestamoRepository prestamoRepository, CuotaRepository cuotaRepository,
                                   PagoRepository pagoRepository, ServiceAmortizacion servicioAmortizacion,
                                   ServiceFechaNegocio servicioFechaNegocio, ServiceParametro servicioParametro,
                                   ServiceAnalista servicioAnalista, ServiceContabilidad servicioContabilidad) {
        this.prestamoRepository = prestamoRepository;
        this.cuotaRepository = cuotaRepository;
        this.pagoRepository = pagoRepository;
        this.servicioAmortizacion = servicioAmortizacion;
        this.servicioFechaNegocio = servicioFechaNegocio;
        this.servicioParametro = servicioParametro;
        this.servicioAnalista = servicioAnalista;
        this.servicioContabilidad = servicioContabilidad;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoAbono simular(String numeroOperacion, BigDecimal monto, ModalidadAbono modalidad) {
        Prestamo prestamo = prestamoRepository.findByNumeroOperacion(numeroOperacion)
                .orElseThrow(() -> operacionInexistente(numeroOperacion));
        return resultado(prestamo, calcular(prestamo, monto, modalidad));
    }

    @Override
    @Transactional
    public ResultadoAbono registrar(String numeroOperacion, BigDecimal monto, ModalidadAbono modalidad,
                                    String cedulaAnalista) {
        SesionAnalista analista = servicioAnalista.findByNationalId(cedulaAnalista)
                .orElseThrow(() -> new PrestamoException(HttpStatus.FORBIDDEN,
                        "El analista " + cedulaAnalista + " no está registrado."));
        Prestamo prestamo = prestamoRepository.findParaActualizar(numeroOperacion)
                .orElseThrow(() -> operacionInexistente(numeroOperacion));
        Calculo calculo = calcular(prestamo, monto, modalidad);
        ResultadoAbono resultado = resultado(prestamo, calculo);

        aplicarTabla(calculo, analista.cedula());
        prestamo.setPrincipalBalance(calculo.saldoNuevo());
        prestamo.setLastModifiedBy(analista.cedula());
        prestamo.setLastModifiedAt(LocalDateTime.now());

        Pago pago = new Pago(prestamo.getId(), analista.idAnalista(), OrigenPago.ABONO_EXTRAORDINARIO,
                calculo.fecha(), monto, modalidad, analista.cedula());
        pago.agregarDetalle(null, BigDecimal.ZERO, BigDecimal.ZERO, calculo.interes(), calculo.capital());
        pagoRepository.save(pago);

        contabilizar(prestamo, pago, calculo);
        return resultado;
    }

    private Calculo calcular(Prestamo prestamo, BigDecimal monto, ModalidadAbono modalidad) {
        validarMonto(monto, modalidad);
        validarEstado(prestamo);
        LocalDate fecha = servicioFechaNegocio.getCurrentBusinessDate();
        List<Cuota> cuotas = cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(prestamo.getId());
        List<Cuota> pendientes = cuotasPorVencer(cuotas, fecha);

        if (pagoRepository.existsByIdOperacionAndOrigenAndFechaPago(prestamo.getId(),
                OrigenPago.ABONO_EXTRAORDINARIO, fecha)) {
            throw new PrestamoException(HttpStatus.CONFLICT,
                    "La operación ya tiene un abono extraordinario registrado hoy.");
        }

        long dias = ChronoUnit.DAYS.between(inicioDelPeriodo(prestamo, cuotas, fecha), fecha);
        BigDecimal saldo = prestamo.getPrincipalBalance();
        BigDecimal interes = servicioAmortizacion.interesPorDias(saldo, prestamo.getEffectiveAnnualRate(), dias,
                baseDias());
        BigDecimal minimo = servicioParametro.getValue(ABONO_MINIMO);
        if (monto.compareTo(minimo) < 0) {
            throw new PrestamoException(HttpStatus.BAD_REQUEST,
                    "El abono mínimo es USD " + minimo.setScale(2, RoundingMode.HALF_UP) + ".");
        }
        if (monto.compareTo(interes) <= 0) {
            throw new PrestamoException(HttpStatus.BAD_REQUEST,
                    "El abono debe ser mayor al interés devengado a la fecha (USD " + interes + ").");
        }
        BigDecimal capital = monto.subtract(interes);
        if (capital.compareTo(saldo) >= 0) {
            throw new PrestamoException(HttpStatus.CONFLICT,
                    "El abono cubre toda la deuda; registre una cancelación anticipada.");
        }

        BigDecimal saldoNuevo = saldo.subtract(capital);
        List<CuotaCalculada> nuevas = servicioAmortizacion.recalcular(new RecalculoTabla(saldoNuevo,
                prestamo.getEffectiveAnnualRate(), prestamo.getAmortizationMethod(), modalidad, fecha,
                pendientes.getFirst().getInstallmentNumber(),
                pendientes.stream().map(Cuota::getDueDate).toList(),
                cuotaVigente(prestamo, pendientes.getFirst()), baseDias()));
        return new Calculo(fecha, modalidad, monto, interes, capital, saldo, saldoNuevo, pendientes, nuevas);
    }

    private void validarMonto(BigDecimal monto, ModalidadAbono modalidad) {
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new PrestamoException(HttpStatus.BAD_REQUEST,
                    "El valor del abono debe ser mayor que cero y tener máximo dos decimales.");
        }
        if (modalidad == null) {
            throw new PrestamoException(HttpStatus.BAD_REQUEST, "La modalidad del abono es obligatoria.");
        }
    }

    private void validarEstado(Prestamo prestamo) {
        EstadoCartera estado = prestamo.getPortfolioStatus();
        if (estado == EstadoCartera.CANCELADA || estado == EstadoCartera.CASTIGADA) {
            throw new PrestamoException(HttpStatus.CONFLICT,
                    "No se puede abonar a una operación en estado " + estado + ".");
        }
        if (estado != EstadoCartera.POR_VENCER) {
            throw cuotasVencidas();
        }
    }

    private List<Cuota> cuotasPorVencer(List<Cuota> cuotas, LocalDate fecha) {
        if (cuotas.stream().anyMatch(cuota -> cuota.getDueDate().isBefore(fecha) && noPagada(cuota))) {
            throw cuotasVencidas();
        }
        if (cuotas.stream().anyMatch(cuota -> cuota.getDueDate().isEqual(fecha) && noPagada(cuota))) {
            throw new PrestamoException(HttpStatus.CONFLICT,
                    "La cuota que vence hoy debe pagarse antes de registrar el abono.");
        }
        List<Cuota> pendientes = cuotas.stream().filter(cuota -> cuota.getDueDate().isAfter(fecha)).toList();
        if (pendientes.isEmpty()) {
            throw new PrestamoException(HttpStatus.CONFLICT, "La operación no tiene cuotas por vencer.");
        }
        if (pendientes.stream().anyMatch(cuota -> cuota.getStatus() != EstadoCuota.PENDIENTE)) {
            throw new PrestamoException(HttpStatus.CONFLICT,
                    "La operación tiene cuotas futuras pagadas por adelantado; no se puede recalcular la tabla.");
        }
        return pendientes;
    }

    private LocalDate inicioDelPeriodo(Prestamo prestamo, List<Cuota> cuotas, LocalDate fecha) {
        LocalDate ultimoVencimiento = cuotas.stream()
                .map(Cuota::getDueDate)
                .filter(vencimiento -> !vencimiento.isAfter(fecha))
                .max(LocalDate::compareTo)
                .orElse(prestamo.getDisbursementDate());
        return pagoRepository.findUltimaFecha(prestamo.getId(), OrigenPago.ABONO_EXTRAORDINARIO)
                .filter(ultimoAbono -> ultimoAbono.isAfter(ultimoVencimiento))
                .orElse(ultimoVencimiento);
    }

    private BigDecimal cuotaVigente(Prestamo prestamo, Cuota proxima) {
        return prestamo.getAmortizationMethod() == MetodoAmortizacion.FRANCES
                ? proxima.getPrincipalAmount().add(proxima.getInterestAmount())
                : proxima.getPrincipalAmount();
    }

    private Integer baseDias() {
        return servicioParametro.getValue(BASE_DIAS).intValue();
    }

    private void aplicarTabla(Calculo calculo, String usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        List<Cuota> eliminadas = new ArrayList<>();
        for (int index = 0; index < calculo.pendientes().size(); index++) {
            Cuota cuota = calculo.pendientes().get(index);
            if (index >= calculo.nuevas().size()) {
                eliminadas.add(cuota);
                continue;
            }
            CuotaCalculada nueva = calculo.nuevas().get(index);
            cuota.setPrincipalAmount(nueva.capital());
            cuota.setInterestAmount(nueva.interes());
            cuota.setRemainingPrincipalBalance(nueva.remainingPrincipalBalance());
            cuota.setLastModifiedBy(usuario);
            cuota.setLastModifiedAt(ahora);
        }
        cuotaRepository.deleteAll(eliminadas);
    }

    private void contabilizar(Prestamo prestamo, Pago pago, Calculo calculo) {
        List<MovimientoContable> movimientos = new ArrayList<>();
        movimientos.add(new MovimientoContable(prestamo.getId(), calculo.fecha(), TipoAsiento.PAGO,
                CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, CodigoCuentaContable.CARTERA_POR_VENCER,
                calculo.capital(), "ABONO-" + pago.getId() + "-CAPITAL"));
        if (calculo.interes().signum() > 0) {
            movimientos.add(new MovimientoContable(prestamo.getId(), calculo.fecha(), TipoAsiento.PAGO,
                    CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, CodigoCuentaContable.INTERESES_POR_COBRAR,
                    calculo.interes(), "ABONO-" + pago.getId() + "-INTERES"));
        }
        servicioContabilidad.registrarAsientos(movimientos);
    }

    private ResultadoAbono resultado(Prestamo prestamo, Calculo calculo) {
        BigDecimal seguro = prestamo.getMonthlyInsurance();
        List<ResumenCuota> antes = calculo.pendientes().stream()
                .map(cuota -> resumen(cuota.getInstallmentNumber(), cuota.getDueDate(), cuota.getPrincipalAmount(),
                        cuota.getInterestAmount(), seguro, cuota.getRemainingPrincipalBalance()))
                .toList();
        List<ResumenCuota> despues = calculo.nuevas().stream()
                .map(cuota -> resumen(cuota.numeroCuota(), cuota.fechaVencimiento(), cuota.capital(),
                        cuota.interes(), seguro, cuota.remainingPrincipalBalance()))
                .toList();
        return new ResultadoAbono(prestamo.getLoanNumber(), calculo.fecha(), calculo.modalidad(), calculo.monto(),
                calculo.interes(), calculo.capital(), calculo.saldoAnterior(), calculo.saldoNuevo(),
                cuadro(antes), cuadro(despues));
    }

    private ResumenCuota resumen(Integer numero, LocalDate vencimiento, BigDecimal capital, BigDecimal interes,
                                 BigDecimal seguro, BigDecimal saldo) {
        return new ResumenCuota(numero, vencimiento, capital, interes, seguro, capital.add(interes).add(seguro),
                saldo, EstadoCuota.PENDIENTE.name());
    }

    private CuadroAmortizacion cuadro(List<ResumenCuota> cuotas) {
        BigDecimal totalInteres = cuotas.stream().map(ResumenCuota::valorInteres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CuadroAmortizacion(cuotas, cuotas.size(), cuotas.getFirst().valorCuota(), totalInteres,
                cuotas.getLast().fechaVencimiento());
    }

    private boolean noPagada(Cuota cuota) {
        return cuota.getStatus() != EstadoCuota.PAGADA;
    }

    private PrestamoException cuotasVencidas() {
        return new PrestamoException(HttpStatus.CONFLICT,
                "La operación tiene cuotas vencidas; el valor debe aplicarse como pago con prelación, no como abono.");
    }

    private PrestamoException operacionInexistente(String numeroOperacion) {
        return new PrestamoException(HttpStatus.NOT_FOUND, "La operación " + numeroOperacion + " no existe.");
    }

    private record Calculo(LocalDate fecha, ModalidadAbono modalidad, BigDecimal monto, BigDecimal interes,
                           BigDecimal capital, BigDecimal saldoAnterior, BigDecimal saldoNuevo,
                           List<Cuota> pendientes, List<CuotaCalculada> nuevas) {
    }
}
