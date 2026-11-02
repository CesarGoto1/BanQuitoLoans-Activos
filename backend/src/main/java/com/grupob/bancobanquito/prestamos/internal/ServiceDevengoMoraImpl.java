package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ServiceDevengoMora;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceDevengoMoraImpl implements ServiceDevengoMora {

    private static final String PARAMETRO_FACTOR_MORA = "PAR-10";
    private static final String PARAMETRO_BASE_DIAS = "PAR-11";
    private static final String USUARIO_CIERRE = "proceso-cierre";
    private static final BigDecimal MESES_POR_ANIO = BigDecimal.valueOf(12);
    private static final int ESCALA_DIARIA = 4;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA_DIARIA);

    private final PrestamoRepository prestamoRepository;
    private final CuotaRepository cuotaRepository;
    private final DevengoDiarioRepository devengoRepository;
    private final MoraDiariaRepository moraRepository;
    private final PagoRepository pagoRepository;
    private final ServiceAmortizacion servicioAmortizacion;
    private final ServiceParametro servicioParametro;
    private final ServiceContabilidad servicioContabilidad;

    ServiceDevengoMoraImpl(PrestamoRepository prestamoRepository, CuotaRepository cuotaRepository,
                           DevengoDiarioRepository devengoRepository, MoraDiariaRepository moraRepository,
                           PagoRepository pagoRepository, ServiceAmortizacion servicioAmortizacion,
                           ServiceParametro servicioParametro, ServiceContabilidad servicioContabilidad) {
        this.prestamoRepository = prestamoRepository;
        this.cuotaRepository = cuotaRepository;
        this.devengoRepository = devengoRepository;
        this.moraRepository = moraRepository;
        this.pagoRepository = pagoRepository;
        this.servicioAmortizacion = servicioAmortizacion;
        this.servicioParametro = servicioParametro;
        this.servicioContabilidad = servicioContabilidad;
    }

    // Devenga un día de interés por operación vigente. El devengo empieza el día siguiente al desembolso.
    // Si la operación recibió un abono extraordinario ese día, el abono ya liquidó el interés hasta la fecha
    // inclusive, así que solo se devenga la diferencia entre lo liquidado y lo ya devengado en el periodo.
    @Override
    @Transactional
    public int ejecutarDevengo(LocalDate fechaNegocio) {
        int baseDias = baseDias(fechaNegocio);
        Map<Integer, BigDecimal> interesAbonosDelDia = interesLiquidadoPorAbonos(fechaNegocio);
        Map<BigDecimal, BigDecimal> tasasNominales = new HashMap<>();
        int procesados = 0;
        for (Prestamo prestamo : prestamosVigentes()) {
            if (!fechaNegocio.isAfter(prestamo.getDisbursementDate())
                    || devengoRepository.existsByPrestamoIdAndFechaDevengo(prestamo.getId(), fechaNegocio)) {
                continue;
            }

            BigDecimal interesAbono = interesAbonosDelDia.get(prestamo.getId());
            BigDecimal interesDia = interesAbono == null
                    ? CalculoDevengo.interesDiario(prestamo.getPrincipalBalance(),
                            tasaNominal(prestamo, tasasNominales), baseDias)
                    : diferenciaConAbono(prestamo, fechaNegocio, interesAbono);
            devengoRepository.save(new DevengoDiario(prestamo, fechaNegocio, interesDia, USUARIO_CIERRE));
            registrarAsientoDevengo(prestamo, fechaNegocio, interesDia);
            procesados++;
        }
        return procesados;
    }

    // En la fecha de vencimiento ajusta la diferencia entre el interés de la cuota y lo devengado en su periodo.
    @Override
    @Transactional
    public int ejecutarAjustesDevengo(LocalDate fechaNegocio) {
        int procesados = 0;
        for (Prestamo prestamo : prestamosVigentes()) {
            List<Cuota> cuotas = cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(prestamo.getId());
            for (int indice = 0; indice < cuotas.size(); indice++) {
                Cuota cuota = cuotas.get(indice);
                if (!fechaNegocio.equals(cuota.getDueDate())) {
                    continue;
                }

                LocalDate vencimientoAnterior = indice == 0
                        ? prestamo.getDisbursementDate()
                        : cuotas.get(indice - 1).getDueDate();
                LocalDate inicioPeriodo = pagoRepository
                        .findUltimaFechaAntesDe(prestamo.getId(), OrigenPago.ABONO_EXTRAORDINARIO, fechaNegocio)
                        .filter(fechaAbono -> fechaAbono.isAfter(vencimientoAnterior))
                        .orElse(vencimientoAnterior);
                BigDecimal devengado = devengoRepository.sumarInteresDevengado(
                        prestamo.getId(), inicioPeriodo.plusDays(1), fechaNegocio);
                BigDecimal ajuste = cuota.getInterestAmount().subtract(devengado)
                        .setScale(2, RoundingMode.HALF_UP);
                cuota.setInterestAccrualAdjustment(ajuste);
                cuotaRepository.save(cuota);
                registrarAsientoAjuste(prestamo, cuota, fechaNegocio, ajuste);
                procesados++;
            }
        }
        return procesados;
    }

    // La mora corre desde el día siguiente al vencimiento sobre el capital impago de cada cuota vencida.
    @Override
    @Transactional
    public int ejecutarMora(LocalDate fechaNegocio) {
        BigDecimal factorMora = servicioParametro.getValue(PARAMETRO_FACTOR_MORA, fechaNegocio);
        int baseDias = baseDias(fechaNegocio);
        Map<BigDecimal, BigDecimal> tasasNominales = new HashMap<>();
        int procesados = 0;
        for (Prestamo prestamo : prestamosVigentes()) {
            for (Cuota cuota : cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(prestamo.getId())) {
                if (!estaVencidaImpaga(cuota, fechaNegocio)
                        || moraRepository.existsByCuotaIdAndFechaMora(cuota.getId(), fechaNegocio)) {
                    continue;
                }

                BigDecimal capitalVencido = cuota.getPrincipalAmount()
                        .subtract(cuota.getPrincipalPaid()).max(BigDecimal.ZERO);
                BigDecimal interesMora = capitalVencido.signum() == 0
                        ? CERO
                        : CalculoMora.interesDiario(capitalVencido, tasaNominal(prestamo, tasasNominales),
                                factorMora, baseDias);
                moraRepository.save(new MoraDiaria(cuota, fechaNegocio, interesMora, USUARIO_CIERRE));
                cuota.setAccruedDefaultInterest(cuota.getAccruedDefaultInterest().add(interesMora));
                cuotaRepository.save(cuota);
                registrarAsientoMora(prestamo, cuota, fechaNegocio, interesMora);
                procesados++;
            }
        }
        return procesados;
    }

    private List<Prestamo> prestamosVigentes() {
        return prestamoRepository.findByPortfolioStatusNotIn(List.of(EstadoCartera.CASTIGADA, EstadoCartera.CANCELADA));
    }

    private Map<Integer, BigDecimal> interesLiquidadoPorAbonos(LocalDate fechaNegocio) {
        Map<Integer, BigDecimal> interesPorOperacion = new HashMap<>();
        for (Pago abono : pagoRepository.findByOrigenAndFechaPago(OrigenPago.ABONO_EXTRAORDINARIO, fechaNegocio)) {
            BigDecimal interes = abono.getDetalles().stream()
                    .map(DetallePago::getAplicadoInteres)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            interesPorOperacion.merge(abono.getIdOperacion(), interes, BigDecimal::add);
        }
        return interesPorOperacion;
    }

    // Mismo periodo que usa el abono: desde el último vencimiento (o abono anterior) hasta la fecha del abono.
    private BigDecimal diferenciaConAbono(Prestamo prestamo, LocalDate fechaNegocio, BigDecimal interesAbono) {
        LocalDate ultimoVencimiento = cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(prestamo.getId()).stream()
                .map(Cuota::getDueDate)
                .filter(vencimiento -> !vencimiento.isAfter(fechaNegocio))
                .max(LocalDate::compareTo)
                .orElse(prestamo.getDisbursementDate());
        LocalDate inicioPeriodo = pagoRepository
                .findUltimaFechaAntesDe(prestamo.getId(), OrigenPago.ABONO_EXTRAORDINARIO, fechaNegocio)
                .filter(abonoAnterior -> abonoAnterior.isAfter(ultimoVencimiento))
                .orElse(ultimoVencimiento);
        BigDecimal yaDevengado = devengoRepository.sumarInteresDevengado(
                prestamo.getId(), inicioPeriodo.plusDays(1), fechaNegocio.minusDays(1));
        return interesAbono.subtract(yaDevengado).max(BigDecimal.ZERO).setScale(ESCALA_DIARIA, RoundingMode.HALF_UP);
    }

    private boolean estaVencidaImpaga(Cuota cuota, LocalDate fechaNegocio) {
        return cuota.getDueDate().isBefore(fechaNegocio) && cuota.getStatus() != EstadoCuota.PAGADA;
    }

    private int baseDias(LocalDate fechaNegocio) {
        return servicioParametro.getValue(PARAMETRO_BASE_DIAS, fechaNegocio).intValueExact();
    }

    private BigDecimal tasaNominal(Prestamo prestamo, Map<BigDecimal, BigDecimal> tasasNominales) {
        return tasasNominales.computeIfAbsent(prestamo.getEffectiveAnnualRate(),
                tea -> servicioAmortizacion.monthlyRate(tea).multiply(MESES_POR_ANIO));
    }

    private static boolean enSuspenso(Prestamo prestamo) {
        return prestamo.getPortfolioStatus() == EstadoCartera.EN_LITIGIO;
    }

    private void registrarAsientoDevengo(Prestamo prestamo, LocalDate fechaNegocio, BigDecimal interesDia) {
        BigDecimal monto = interesDia.setScale(2, RoundingMode.HALF_UP);
        if (monto.signum() == 0) {
            return;
        }
        boolean suspenso = enSuspenso(prestamo);
        servicioContabilidad.registrarAsiento(new MovimientoContable(prestamo.getId(), fechaNegocio,
                suspenso ? TipoAsiento.DEVENGO_SUSPENSO : TipoAsiento.DEVENGO,
                suspenso ? CodigoCuentaContable.INTERESES_EN_SUSPENSO : CodigoCuentaContable.INTERESES_POR_COBRAR,
                suspenso ? CodigoCuentaContable.CONTRA_CUENTAS_ORDEN : CodigoCuentaContable.INGRESOS_INTERESES,
                monto, "DEVENGO:" + prestamo.getId() + ":" + fechaNegocio));
    }

    private void registrarAsientoMora(Prestamo prestamo, Cuota cuota, LocalDate fechaNegocio,
                                      BigDecimal interesMora) {
        BigDecimal monto = interesMora.setScale(2, RoundingMode.HALF_UP);
        if (monto.signum() == 0) {
            return;
        }
        boolean suspenso = enSuspenso(prestamo);
        servicioContabilidad.registrarAsiento(new MovimientoContable(prestamo.getId(), fechaNegocio, TipoAsiento.MORA,
                suspenso ? CodigoCuentaContable.INTERESES_EN_SUSPENSO : CodigoCuentaContable.INTERESES_MORA_POR_COBRAR,
                suspenso ? CodigoCuentaContable.CONTRA_CUENTAS_ORDEN : CodigoCuentaContable.INGRESOS_MORA,
                monto, "MORA:" + cuota.getId() + ":" + fechaNegocio));
    }

    private void registrarAsientoAjuste(Prestamo prestamo, Cuota cuota, LocalDate fechaNegocio, BigDecimal ajuste) {
        if (ajuste.signum() == 0) {
            return;
        }
        boolean suspenso = enSuspenso(prestamo);
        CodigoCuentaContable cuentaDeudora = suspenso
                ? CodigoCuentaContable.INTERESES_EN_SUSPENSO
                : CodigoCuentaContable.INTERESES_POR_COBRAR;
        CodigoCuentaContable cuentaAcreedora = suspenso
                ? CodigoCuentaContable.CONTRA_CUENTAS_ORDEN
                : CodigoCuentaContable.INGRESOS_INTERESES;
        boolean positivo = ajuste.signum() > 0;
        servicioContabilidad.registrarAsiento(new MovimientoContable(prestamo.getId(), fechaNegocio,
                TipoAsiento.AJUSTE_DEVENGO,
                positivo ? cuentaDeudora : cuentaAcreedora,
                positivo ? cuentaAcreedora : cuentaDeudora,
                ajuste.abs(), "AJUSTE_DEVENGO:" + cuota.getId() + ":" + fechaNegocio));
    }
}
