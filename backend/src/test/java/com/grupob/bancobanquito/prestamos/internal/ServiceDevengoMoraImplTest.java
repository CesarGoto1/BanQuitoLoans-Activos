package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServiceDevengoMoraImplTest {

    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 10, 31);
    private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 11, 30);

    private PrestamoRepository prestamoRepository;
    private CuotaRepository cuotaRepository;
    private DevengoDiarioRepository devengoRepository;
    private MoraDiariaRepository moraRepository;
    private PagoRepository pagoRepository;
    private ServiceAmortizacion servicioAmortizacion;
    private ServiceParametro servicioParametro;
    private ServiceContabilidad servicioContabilidad;
    private ServiceDevengoMoraImpl servicio;

    @BeforeEach
    void setUp() {
        prestamoRepository = mock(PrestamoRepository.class);
        cuotaRepository = mock(CuotaRepository.class);
        devengoRepository = mock(DevengoDiarioRepository.class);
        moraRepository = mock(MoraDiariaRepository.class);
        pagoRepository = mock(PagoRepository.class);
        servicioAmortizacion = mock(ServiceAmortizacion.class);
        servicioParametro = mock(ServiceParametro.class);
        servicioContabilidad = mock(ServiceContabilidad.class);
        servicio = new ServiceDevengoMoraImpl(prestamoRepository, cuotaRepository, devengoRepository,
                moraRepository, pagoRepository, servicioAmortizacion, servicioParametro, servicioContabilidad);
        when(servicioParametro.getValue(eq("PAR-11"), any(LocalDate.class))).thenReturn(new BigDecimal("360.0000"));
        when(servicioParametro.getValue(eq("PAR-10"), any(LocalDate.class))).thenReturn(new BigDecimal("1.10"));
        when(servicioAmortizacion.monthlyRate(any())).thenReturn(new BigDecimal("0.0130035"));
        when(pagoRepository.findUltimaFechaAntesDe(anyInt(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void registraDevengoDesdeElDiaSiguienteAlDesembolso() {
        Prestamo prestamo = prestamo(1, EstadoCartera.POR_VENCER);
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));

        assertThat(servicio.ejecutarDevengo(DESEMBOLSO.plusDays(1))).isEqualTo(1);

        ArgumentCaptor<DevengoDiario> devengo = ArgumentCaptor.forClass(DevengoDiario.class);
        verify(devengoRepository).save(devengo.capture());
        assertThat(devengo.getValue().getInteresDevengado()).isEqualByComparingTo("3.9984");
        verify(servicioContabilidad).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void noRegistraDevengoElMismoDiaDelDesembolso() {
        Prestamo prestamo = prestamo(1, EstadoCartera.POR_VENCER);
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));

        assertThat(servicio.ejecutarDevengo(DESEMBOLSO)).isZero();

        verify(devengoRepository, never()).save(any(DevengoDiario.class));
        verify(servicioContabilidad, never()).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void noDuplicaDevengoCuandoLaFechaYaFueProcesada() {
        Prestamo prestamo = prestamo(1, EstadoCartera.POR_VENCER);
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(devengoRepository.existsByPrestamoIdAndFechaDevengo(1, FECHA_NEGOCIO)).thenReturn(true);

        assertThat(servicio.ejecutarDevengo(FECHA_NEGOCIO)).isZero();

        verify(devengoRepository, never()).save(any(DevengoDiario.class));
        verify(servicioContabilidad, never()).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void noConsideraLasOperacionesCanceladasNiCastigadas() {
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of());

        servicio.ejecutarDevengo(FECHA_NEGOCIO);

        verify(prestamoRepository).findByPortfolioStatusNotIn(
                List.of(EstadoCartera.CASTIGADA, EstadoCartera.CANCELADA));
    }

    @Test
    void elDiaDelAbonoDevengaSoloLaDiferenciaConElInteresQueLiquidoElAbono() {
        LocalDate fechaAbono = LocalDate.of(2026, 11, 15);
        Prestamo prestamo = prestamo(1, EstadoCartera.POR_VENCER);
        prestamo.setPrincipalBalance(new BigDecimal("5000.00"));
        Cuota cuota = cuota(10, prestamo, FECHA_NEGOCIO, new BigDecimal("50.00"));
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(cuota));
        when(pagoRepository.findByOrigenAndFechaPago(OrigenPago.ABONO_EXTRAORDINARIO, fechaAbono))
                .thenReturn(List.of(abono(1, fechaAbono, new BigDecimal("59.98"))));
        when(devengoRepository.sumarInteresDevengado(1, DESEMBOLSO.plusDays(1), fechaAbono.minusDays(1)))
                .thenReturn(new BigDecimal("55.9790"));

        assertThat(servicio.ejecutarDevengo(fechaAbono)).isEqualTo(1);

        ArgumentCaptor<DevengoDiario> devengo = ArgumentCaptor.forClass(DevengoDiario.class);
        verify(devengoRepository).save(devengo.capture());
        assertThat(devengo.getValue().getInteresDevengado()).isEqualByComparingTo("4.0010");
    }

    @Test
    void registraElAjusteAlVencimientoDeLaCuota() {
        Prestamo prestamo = prestamo(1, EstadoCartera.POR_VENCER);
        Cuota cuota = cuota(10, prestamo, FECHA_NEGOCIO, new BigDecimal("130.03"));
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(cuota));
        when(devengoRepository.sumarInteresDevengado(1, LocalDate.of(2026, 11, 1), FECHA_NEGOCIO))
                .thenReturn(new BigDecimal("130.0350"));

        assertThat(servicio.ejecutarAjustesDevengo(FECHA_NEGOCIO)).isEqualTo(1);

        assertThat(cuota.getInterestAccrualAdjustment()).isEqualByComparingTo("-0.01");
        ArgumentCaptor<MovimientoContable> asiento = ArgumentCaptor.forClass(MovimientoContable.class);
        verify(servicioContabilidad).registrarAsiento(asiento.capture());
        assertThat(asiento.getValue().monto()).isEqualByComparingTo("0.01");
    }

    @Test
    void despuesDeUnAbonoElAjusteSoloComparaLoDevengadoDesdeElAbono() {
        LocalDate fechaAbono = LocalDate.of(2026, 11, 15);
        Prestamo prestamo = prestamo(1, EstadoCartera.POR_VENCER);
        Cuota cuota = cuota(10, prestamo, FECHA_NEGOCIO, new BigDecimal("40.00"));
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(cuota));
        when(pagoRepository.findUltimaFechaAntesDe(1, OrigenPago.ABONO_EXTRAORDINARIO, FECHA_NEGOCIO))
                .thenReturn(Optional.of(fechaAbono));
        when(devengoRepository.sumarInteresDevengado(1, fechaAbono.plusDays(1), FECHA_NEGOCIO))
                .thenReturn(new BigDecimal("40.0000"));

        assertThat(servicio.ejecutarAjustesDevengo(FECHA_NEGOCIO)).isEqualTo(1);

        assertThat(cuota.getInterestAccrualAdjustment()).isEqualByComparingTo("0.00");
        verify(servicioContabilidad, never()).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void registraMoraSobreElCapitalPendienteDeLaCuota() {
        LocalDate fechaNegocio = LocalDate.of(2026, 12, 9);
        Prestamo prestamo = prestamo(1, EstadoCartera.VENCIDA);
        Cuota cuota = cuota(10, prestamo, FECHA_NEGOCIO, new BigDecimal("130.03"));
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(cuota));

        assertThat(servicio.ejecutarMora(fechaNegocio)).isEqualTo(1);

        assertThat(cuota.getAccruedDefaultInterest()).isEqualByComparingTo("0.3697");
        verify(moraRepository).save(any(MoraDiaria.class));
    }

    @Test
    void laMoraEmpiezaElDiaSiguienteAlVencimientoAunqueLaCarteraSigaPorVencer() {
        Prestamo prestamo = prestamo(1, EstadoCartera.POR_VENCER);
        Cuota cuota = cuota(10, prestamo, FECHA_NEGOCIO, new BigDecimal("130.03"));
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(cuota));

        assertThat(servicio.ejecutarMora(FECHA_NEGOCIO)).isZero();
        assertThat(servicio.ejecutarMora(FECHA_NEGOCIO.plusDays(1))).isEqualTo(1);

        verify(moraRepository, times(1)).save(any(MoraDiaria.class));
    }

    @Test
    void procesaCadaCuotaVencidaDeLaOperacion() {
        LocalDate fechaNegocio = LocalDate.of(2026, 12, 9);
        Prestamo prestamo = prestamo(1, EstadoCartera.VENCIDA);
        Cuota primera = cuota(10, prestamo, LocalDate.of(2026, 11, 30), new BigDecimal("130.03"));
        Cuota segunda = cuota(11, prestamo, LocalDate.of(2026, 11, 30), new BigDecimal("119.95"));
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(primera, segunda));

        assertThat(servicio.ejecutarMora(fechaNegocio)).isEqualTo(2);

        verify(moraRepository, times(2)).save(any(MoraDiaria.class));
        verify(servicioContabilidad, times(2)).registrarAsiento(any(MovimientoContable.class));
    }

    @Test
    void noRegistraMoraParaUnaCuotaPagada() {
        LocalDate fechaNegocio = LocalDate.of(2026, 12, 9);
        Prestamo prestamo = prestamo(1, EstadoCartera.VENCIDA);
        Cuota cuota = cuota(10, prestamo, LocalDate.of(2026, 11, 30), new BigDecimal("130.03"));
        cuota.setStatus(EstadoCuota.PAGADA);
        when(prestamoRepository.findByPortfolioStatusNotIn(anyList())).thenReturn(List.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(cuota));

        assertThat(servicio.ejecutarMora(fechaNegocio)).isZero();

        verify(moraRepository, never()).save(any(MoraDiaria.class));
        verify(servicioContabilidad, never()).registrarAsiento(any(MovimientoContable.class));
    }

    private Prestamo prestamo(Integer id, EstadoCartera estado) {
        Prestamo prestamo = new Prestamo();
        prestamo.setId(id);
        prestamo.setDisbursementDate(DESEMBOLSO);
        prestamo.setEffectiveAnnualRate(new BigDecimal("0.1677"));
        prestamo.setPrincipalBalance(new BigDecimal("9224.59"));
        prestamo.setPortfolioStatus(estado);
        return prestamo;
    }

    private Cuota cuota(Integer id, Prestamo prestamo, LocalDate vencimiento, BigDecimal interes) {
        Cuota cuota = new Cuota();
        cuota.setId(id);
        cuota.setLoan(prestamo);
        cuota.setDueDate(vencimiento);
        cuota.setInterestAmount(interes);
        cuota.setPrincipalAmount(new BigDecimal("775.41"));
        cuota.setPrincipalPaid(BigDecimal.ZERO);
        cuota.setAccruedDefaultInterest(BigDecimal.ZERO.setScale(4));
        cuota.setStatus(EstadoCuota.PENDIENTE);
        return cuota;
    }

    private Pago abono(Integer idOperacion, LocalDate fecha, BigDecimal interes) {
        Pago pago = new Pago(idOperacion, 1, OrigenPago.ABONO_EXTRAORDINARIO, fecha, new BigDecimal("1000.00"),
                null, "1700000001");
        pago.agregarDetalle(null, BigDecimal.ZERO, BigDecimal.ZERO, interes, new BigDecimal("1000.00").subtract(interes));
        return pago;
    }
}
