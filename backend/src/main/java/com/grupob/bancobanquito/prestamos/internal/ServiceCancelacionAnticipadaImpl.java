package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.LiquidacionCancelacion;
import com.grupob.bancobanquito.prestamos.ServiceCancelacionAnticipada;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceCancelacionAnticipadaImpl implements ServiceCancelacionAnticipada {

    private static final String BASE_DIAS = "PAR-11";

    private final PrestamoRepository prestamoRepository;
    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final CambioEstadoOperacionRepository cambioEstadoRepository;
    private final ServiceAmortizacion servicioAmortizacion;
    private final ServiceFechaNegocio servicioFechaNegocio;
    private final ServiceParametro servicioParametro;
    private final ServiceAnalista servicioAnalista;
    private final ServiceContabilidad servicioContabilidad;

    ServiceCancelacionAnticipadaImpl(PrestamoRepository prestamoRepository, CuotaRepository cuotaRepository,
                                     PagoRepository pagoRepository,
                                     CambioEstadoOperacionRepository cambioEstadoRepository,
                                     ServiceAmortizacion servicioAmortizacion,
                                     ServiceFechaNegocio servicioFechaNegocio, ServiceParametro servicioParametro,
                                     ServiceAnalista servicioAnalista, ServiceContabilidad servicioContabilidad) {
        this.prestamoRepository = prestamoRepository;
        this.cuotaRepository = cuotaRepository;
        this.pagoRepository = pagoRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.servicioAmortizacion = servicioAmortizacion;
        this.servicioFechaNegocio = servicioFechaNegocio;
        this.servicioParametro = servicioParametro;
        this.servicioAnalista = servicioAnalista;
        this.servicioContabilidad = servicioContabilidad;
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidacionCancelacion consultar(String numeroOperacion) {
        Prestamo prestamo = prestamoRepository.findByNumeroOperacion(numeroOperacion)
                .orElseThrow(() -> operacionInexistente(numeroOperacion));
        return liquidacion(prestamo, liquidar(prestamo));
    }

    @Override
    @Transactional
    public LiquidacionCancelacion registrar(String numeroOperacion, BigDecimal montoRecibido,
                                            String cedulaAnalista) {
        SesionAnalista analista = servicioAnalista.findByNationalId(cedulaAnalista)
                .orElseThrow(() -> new PrestamoException(HttpStatus.FORBIDDEN,
                        "El analista " + cedulaAnalista + " no está registrado."));
        Prestamo prestamo = prestamoRepository.findParaActualizar(numeroOperacion)
                .orElseThrow(() -> operacionInexistente(numeroOperacion));
        Liquidacion liquidacion = liquidar(prestamo);
        if (montoRecibido == null || montoRecibido.compareTo(liquidacion.total()) != 0) {
            throw new PrestamoException(HttpStatus.CONFLICT,
                    "El valor de cancelación a la fecha es USD " + liquidacion.total() + ".");
        }
        LiquidacionCancelacion resultado = liquidacion(prestamo, liquidacion);
        EstadoCartera estadoAnterior = prestamo.getPortfolioStatus();
        LocalDateTime ahora = LocalDateTime.now();

        Pago pago = new Pago(prestamo.getId(), analista.idAnalista(), OrigenPago.CANCELACION_ANTICIPADA,
                liquidacion.fecha(), montoRecibido, null, analista.cedula());
        for (Aplicacion aplicacion : liquidacion.aplicaciones()) {
            if (aplicacion.total().signum() > 0) {
                pago.agregarDetalle(aplicacion.cuota().getId(), aplicacion.seguro(), aplicacion.mora(),
                        aplicacion.interes(), aplicacion.capital());
            }
            cerrarCuota(aplicacion, liquidacion.fecha(), analista.cedula(), ahora);
        }
        pagoRepository.save(pago);

        cambioEstadoRepository.save(new CambioEstadoOperacion(prestamo.getId(), liquidacion.fecha(),
                estadoAnterior, EstadoCartera.CANCELADA, prestamo.getDaysPastDue(), analista.cedula()));
        prestamo.setPrincipalBalance(BigDecimal.ZERO);
        prestamo.setPortfolioStatus(EstadoCartera.CANCELADA);
        prestamo.setDaysPastDue(0);
        prestamo.setLastModifiedBy(analista.cedula());
        prestamo.setLastModifiedAt(ahora);

        servicioContabilidad.registrarAsientos(movimientos(prestamo.getId(), estadoAnterior, pago, liquidacion));
        return resultado;
    }

    private Liquidacion liquidar(Prestamo prestamo) {
        if (prestamo.getPortfolioStatus() == EstadoCartera.CANCELADA) {
            throw new PrestamoException(HttpStatus.CONFLICT,
                    "La operación " + prestamo.getLoanNumber() + " ya está cancelada.");
        }
        LocalDate fecha = servicioFechaNegocio.getCurrentBusinessDate();
        List<Cuota> cuotas = cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(prestamo.getId());
        Optional<Cuota> enCurso = cuotas.stream().filter(cuota -> cuota.getDueDate().isAfter(fecha)).findFirst();
        BigDecimal interesEnCurso = enCurso.map(cuota -> interesEnCurso(prestamo, cuotas, cuota, fecha))
                .orElse(BigDecimal.ZERO);

        List<Aplicacion> aplicaciones = new ArrayList<>();
        for (Cuota cuota : cuotas) {
            if (cuota.getStatus() == EstadoCuota.PAGADA) {
                continue;
            }
            BigDecimal capital = cuota.getPrincipalAmount().subtract(cuota.getPrincipalPaid());
            if (cuota.getDueDate().isAfter(fecha)) {
                BigDecimal interes = enCurso.get() == cuota ? interesEnCurso : BigDecimal.ZERO;
                aplicaciones.add(new Aplicacion(cuota, false, BigDecimal.ZERO, BigDecimal.ZERO, interes, capital));
            } else {
                aplicaciones.add(new Aplicacion(cuota, true,
                        cuota.getInsuranceAmount().subtract(cuota.getInsurancePaid()),
                        moraPendiente(cuota),
                        cuota.getInterestAmount().subtract(cuota.getInterestPaid()),
                        capital));
            }
        }
        return new Liquidacion(fecha, aplicaciones);
    }

    private BigDecimal interesEnCurso(Prestamo prestamo, List<Cuota> cuotas, Cuota enCurso, LocalDate fecha) {
        LocalDate ultimoVencimiento = cuotas.stream()
                .map(Cuota::getDueDate)
                .filter(vencimiento -> !vencimiento.isAfter(fecha))
                .max(LocalDate::compareTo)
                .orElse(prestamo.getDisbursementDate());
        LocalDate desde = pagoRepository.findUltimaFecha(prestamo.getId(), OrigenPago.ABONO_EXTRAORDINARIO)
                .filter(ultimoAbono -> ultimoAbono.isAfter(ultimoVencimiento))
                .orElse(ultimoVencimiento);
        BigDecimal base = cuotas.stream()
                .filter(cuota -> cuota.getDueDate().isAfter(fecha))
                .map(Cuota::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal devengado = servicioAmortizacion.interesPorDias(base, prestamo.getEffectiveAnnualRate(),
                ChronoUnit.DAYS.between(desde, fecha), servicioParametro.getValue(BASE_DIAS).intValue());
        return devengado.subtract(enCurso.getInterestPaid()).max(BigDecimal.ZERO);
    }

    private BigDecimal moraPendiente(Cuota cuota) {
        return cuota.getAccruedDefaultInterest().setScale(2, RoundingMode.HALF_UP)
                .subtract(cuota.getDefaultInterestPaid())
                .max(BigDecimal.ZERO);
    }

    private void cerrarCuota(Aplicacion aplicacion, LocalDate fecha, String usuario, LocalDateTime ahora) {
        Cuota cuota = aplicacion.cuota();
        if (!aplicacion.exigible()) {
            cuota.setInterestAmount(cuota.getInterestPaid().add(aplicacion.interes()));
            cuota.setInsuranceAmount(cuota.getInsurancePaid());
        }
        cuota.setInsurancePaid(cuota.getInsuranceAmount());
        cuota.setInterestPaid(cuota.getInterestAmount());
        cuota.setPrincipalPaid(cuota.getPrincipalAmount());
        cuota.setDefaultInterestPaid(cuota.getDefaultInterestPaid().add(aplicacion.mora()));
        cuota.setStatus(EstadoCuota.PAGADA);
        cuota.setPaymentDate(fecha);
        cuota.setLastModifiedBy(usuario);
        cuota.setLastModifiedAt(ahora);
    }

    private List<MovimientoContable> movimientos(Integer idOperacion, EstadoCartera estado, Pago pago,
                                                 Liquidacion liquidacion) {
        String clave = "CANCELACION-" + pago.getId() + "-";
        List<MovimientoContable> movimientos = new ArrayList<>();
        if (estado == EstadoCartera.CASTIGADA) {
            agregar(movimientos, idOperacion, liquidacion.fecha(), TipoAsiento.RECUPERACION,
                    CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, CodigoCuentaContable.INGRESOS_RECUPERACIONES,
                    liquidacion.capital().add(liquidacion.interes()).add(liquidacion.mora()), clave + "RECUPERACION");
            agregar(movimientos, idOperacion, liquidacion.fecha(), TipoAsiento.RECUPERACION,
                    CodigoCuentaContable.CONTRA_CUENTAS_ORDEN, CodigoCuentaContable.CARTERA_CASTIGADA,
                    liquidacion.capital(), clave + "ORDEN");
        } else {
            agregar(movimientos, idOperacion, liquidacion.fecha(), TipoAsiento.PAGO,
                    CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, cuentaCartera(estado),
                    liquidacion.capital(), clave + "CAPITAL");
            agregar(movimientos, idOperacion, liquidacion.fecha(), TipoAsiento.PAGO,
                    CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, CodigoCuentaContable.INTERESES_POR_COBRAR,
                    liquidacion.interes(), clave + "INTERES");
            agregar(movimientos, idOperacion, liquidacion.fecha(), TipoAsiento.PAGO,
                    CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, CodigoCuentaContable.INTERESES_MORA_POR_COBRAR,
                    liquidacion.mora(), clave + "MORA");
        }
        agregar(movimientos, idOperacion, liquidacion.fecha(),
                estado == EstadoCartera.CASTIGADA ? TipoAsiento.RECUPERACION : TipoAsiento.PAGO,
                CodigoCuentaContable.RECAUDACIONES_POR_APLICAR, CodigoCuentaContable.SEGUROS_POR_PAGAR,
                liquidacion.seguro(), clave + "SEGURO");
        return movimientos;
    }

    private void agregar(List<MovimientoContable> movimientos, Integer idOperacion, LocalDate fecha,
                         TipoAsiento tipo, CodigoCuentaContable debito, CodigoCuentaContable credito,
                         BigDecimal monto, String clave) {
        if (monto.signum() > 0) {
            movimientos.add(new MovimientoContable(idOperacion, fecha, tipo, debito, credito, monto, clave));
        }
    }

    private CodigoCuentaContable cuentaCartera(EstadoCartera estado) {
        return switch (estado) {
            case VENCIDA -> CodigoCuentaContable.CARTERA_VENCIDA;
            case EN_LITIGIO -> CodigoCuentaContable.CARTERA_EN_LITIGIO;
            default -> CodigoCuentaContable.CARTERA_POR_VENCER;
        };
    }

    private LiquidacionCancelacion liquidacion(Prestamo prestamo, Liquidacion liquidacion) {
        BigDecimal interesEnCurso = liquidacion.aplicaciones().stream()
                .filter(aplicacion -> !aplicacion.exigible())
                .map(Aplicacion::interes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LiquidacionCancelacion(prestamo.getLoanNumber(), liquidacion.fecha(),
                prestamo.getPortfolioStatus(), liquidacion.capital(), interesEnCurso,
                liquidacion.interes().subtract(interesEnCurso), liquidacion.seguro(), liquidacion.mora(),
                liquidacion.total());
    }

    private PrestamoException operacionInexistente(String numeroOperacion) {
        return new PrestamoException(HttpStatus.NOT_FOUND, "La operación " + numeroOperacion + " no existe.");
    }

    private record Aplicacion(Cuota cuota, boolean exigible, BigDecimal seguro, BigDecimal mora,
                              BigDecimal interes, BigDecimal capital) {

        BigDecimal total() {
            return seguro.add(mora).add(interes).add(capital);
        }
    }

    private record Liquidacion(LocalDate fecha, List<Aplicacion> aplicaciones) {

        BigDecimal capital() {
            return suma(Aplicacion::capital);
        }

        BigDecimal interes() {
            return suma(Aplicacion::interes);
        }

        BigDecimal seguro() {
            return suma(Aplicacion::seguro);
        }

        BigDecimal mora() {
            return suma(Aplicacion::mora);
        }

        BigDecimal total() {
            return capital().add(interes()).add(seguro()).add(mora());
        }

        private BigDecimal suma(Function<Aplicacion, BigDecimal> rubro) {
            return aplicaciones.stream().map(rubro).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
