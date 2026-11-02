package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioClasificacionCarteraTest {

    @Mock
    private OperacionCobranzaRepository operacionRepository;

    @Mock
    private CuotaCobranzaRepository cuotaRepository;

    @Mock
    private TramoCarteraRepository tramoRepository;

    @Mock
    private CambioEstadoOperacionRepository cambioEstadoRepository;

    @Mock
    private ServiceContabilidad serviceContabilidad;

    private ServicioClasificacionCarteraImpl servicio;

    @BeforeEach
    void setUp() {
        this.servicio = new ServicioClasificacionCarteraImpl(
                operacionRepository,
                cuotaRepository,
                tramoRepository,
                cambioEstadoRepository,
                serviceContabilidad
        );
    }

    @Test
    @DisplayName("Debe mantener POR_VENCER cuando no existen cuotas vencidas impagas")
    void debeMantenerPorVencerCuandoNoHayCuotasVencidas() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza operacion = crearOperacion(1, "OP-001", "POR_VENCER", new BigDecimal("5000.00"), 0);

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(operacion));
        when(cuotaRepository.findCuotasVencidasPendientes(1, fechaNegocio)).thenReturn(List.of());

        TramoCartera tramo1 = crearTramo(1, 0, 0, "POR_VENCER", new BigDecimal("0.0100"));
        when(tramoRepository.findTramoPorDiasMora(0, fechaNegocio)).thenReturn(Optional.of(tramo1));

        int total = servicio.clasificarCartera(fechaNegocio);

        assertThat(total).isEqualTo(1);
        assertThat(operacion.getEstadoCartera()).isEqualTo("POR_VENCER");
        assertThat(operacion.getDiasMora()).isEqualTo(0);
        verify(operacionRepository).save(operacion);
        verify(cambioEstadoRepository, never()).save(any());
        verify(serviceContabilidad, never()).registrarAsiento(any());
    }

    @Test
    @DisplayName("Debe reclasificar a VENCIDA cuando la mora esta entre 1 y 30 dias")
    void debeReclasificarAVencidaConMoraMenorA30Dias() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza operacion = crearOperacion(2, "OP-002", "POR_VENCER", new BigDecimal("4500.00"), 0);

        CuotaCobranza cuota = new CuotaCobranza();
        cuota.setIdCuota(10);
        cuota.setIdOperacion(2);
        cuota.setFechaVencimiento(LocalDate.of(2026, 10, 20));
        cuota.setEstadoCuota("PENDIENTE");

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(operacion));
        when(cuotaRepository.findCuotasVencidasPendientes(2, fechaNegocio)).thenReturn(List.of(cuota));

        TramoCartera tramo2 = crearTramo(2, 1, 30, "VENCIDA", new BigDecimal("0.0500"));
        when(tramoRepository.findTramoPorDiasMora(11, fechaNegocio)).thenReturn(Optional.of(tramo2));
        when(cambioEstadoRepository.existsByIdOperacionAndFechaCambio(2, fechaNegocio)).thenReturn(false);

        int total = servicio.clasificarCartera(fechaNegocio);

        assertThat(total).isEqualTo(1);
        assertThat(operacion.getEstadoCartera()).isEqualTo("VENCIDA");
        assertThat(operacion.getDiasMora()).isEqualTo(11);

        verify(cambioEstadoRepository).save(any(CambioEstadoOperacion.class));

        ArgumentCaptor<MovimientoContable> captor = ArgumentCaptor.forClass(MovimientoContable.class);
        verify(serviceContabilidad).registrarAsiento(captor.capture());

        MovimientoContable movimiento = captor.getValue();
        assertThat(movimiento.idOperacion()).isEqualTo(2);
        assertThat(movimiento.fechaAsiento()).isEqualTo(fechaNegocio);
        assertThat(movimiento.tipoAsiento()).isEqualTo(TipoAsiento.RECLASIFICACION);
        assertThat(movimiento.cuentaDebito()).isEqualTo(CodigoCuentaContable.CARTERA_VENCIDA);
        assertThat(movimiento.cuentaCredito()).isEqualTo(CodigoCuentaContable.CARTERA_POR_VENCER);
        assertThat(movimiento.monto()).isEqualByComparingTo(new BigDecimal("4500.00"));
    }

    @Test
    @DisplayName("Debe reclasificar a EN_LITIGIO cuando la mora esta entre 91 y 180 dias")
    void debeReclasificarAEnLitigioConMoraMayorA90Dias() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza operacion = crearOperacion(3, "OP-003", "VENCIDA", new BigDecimal("3000.00"), 45);

        CuotaCobranza cuota = new CuotaCobranza();
        cuota.setIdCuota(20);
        cuota.setIdOperacion(3);
        cuota.setFechaVencimiento(LocalDate.of(2026, 7, 15));
        cuota.setEstadoCuota("PENDIENTE");

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(operacion));
        when(cuotaRepository.findCuotasVencidasPendientes(3, fechaNegocio)).thenReturn(List.of(cuota));

        TramoCartera tramo4 = crearTramo(4, 91, 180, "EN_LITIGIO", new BigDecimal("0.6000"));
        when(tramoRepository.findTramoPorDiasMora(108, fechaNegocio)).thenReturn(Optional.of(tramo4));
        when(cambioEstadoRepository.existsByIdOperacionAndFechaCambio(3, fechaNegocio)).thenReturn(false);

        int total = servicio.clasificarCartera(fechaNegocio);

        assertThat(total).isEqualTo(1);
        assertThat(operacion.getEstadoCartera()).isEqualTo("EN_LITIGIO");
        assertThat(operacion.getDiasMora()).isEqualTo(108);

        ArgumentCaptor<MovimientoContable> captor = ArgumentCaptor.forClass(MovimientoContable.class);
        verify(serviceContabilidad).registrarAsiento(captor.capture());

        MovimientoContable movimiento = captor.getValue();
        assertThat(movimiento.tipoAsiento()).isEqualTo(TipoAsiento.RECLASIFICACION);
        assertThat(movimiento.cuentaDebito()).isEqualTo(CodigoCuentaContable.CARTERA_EN_LITIGIO);
        assertThat(movimiento.cuentaCredito()).isEqualTo(CodigoCuentaContable.CARTERA_VENCIDA);
    }

    @Test
    @DisplayName("Debe castigar la operacion y pasar a cuentas de orden si la mora supera 180 dias")
    void debeCastigarOperacionConMoraMayorA180Dias() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza operacion = crearOperacion(4, "OP-004", "EN_LITIGIO", new BigDecimal("2000.00"), 150);

        CuotaCobranza cuota = new CuotaCobranza();
        cuota.setIdCuota(30);
        cuota.setIdOperacion(4);
        cuota.setFechaVencimiento(LocalDate.of(2026, 4, 1));
        cuota.setEstadoCuota("PENDIENTE");

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(operacion));
        when(cuotaRepository.findCuotasVencidasPendientes(4, fechaNegocio)).thenReturn(List.of(cuota));

        TramoCartera tramo5 = crearTramo(5, 181, null, "CASTIGADA", new BigDecimal("1.0000"));
        when(tramoRepository.findTramoPorDiasMora(213, fechaNegocio)).thenReturn(Optional.of(tramo5));
        when(cambioEstadoRepository.existsByIdOperacionAndFechaCambio(4, fechaNegocio)).thenReturn(false);

        int total = servicio.clasificarCartera(fechaNegocio);

        assertThat(total).isEqualTo(1);
        assertThat(operacion.getEstadoCartera()).isEqualTo("CASTIGADA");
        assertThat(operacion.getDiasMora()).isEqualTo(213);

        ArgumentCaptor<MovimientoContable> captor = ArgumentCaptor.forClass(MovimientoContable.class);
        verify(serviceContabilidad).registrarAsiento(captor.capture());

        MovimientoContable movimiento = captor.getValue();
        assertThat(movimiento.tipoAsiento()).isEqualTo(TipoAsiento.CASTIGO);
        assertThat(movimiento.cuentaDebito()).isEqualTo(CodigoCuentaContable.CARTERA_CASTIGADA);
        assertThat(movimiento.cuentaCredito()).isEqualTo(CodigoCuentaContable.CONTRA_CUENTAS_ORDEN);
        assertThat(movimiento.monto()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(movimiento.claveIdempotencia()).isEqualTo("CASTIGO-ORDEN-4-2026-10-31");
    }

    @Test
    @DisplayName("Debe ser idempotente y no duplicar cambio de estado si ya existe para la fecha")
    void debeSerIdempotenteNoDuplicarCambioEstado() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza operacion = crearOperacion(5, "OP-005", "POR_VENCER", new BigDecimal("1000.00"), 0);

        CuotaCobranza cuota = new CuotaCobranza();
        cuota.setIdCuota(40);
        cuota.setIdOperacion(5);
        cuota.setFechaVencimiento(LocalDate.of(2026, 10, 25));
        cuota.setEstadoCuota("PENDIENTE");

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(operacion));
        when(cuotaRepository.findCuotasVencidasPendientes(5, fechaNegocio)).thenReturn(List.of(cuota));

        TramoCartera tramo2 = crearTramo(2, 1, 30, "VENCIDA", new BigDecimal("0.0500"));
        when(tramoRepository.findTramoPorDiasMora(6, fechaNegocio)).thenReturn(Optional.of(tramo2));
        when(cambioEstadoRepository.existsByIdOperacionAndFechaCambio(5, fechaNegocio)).thenReturn(true);

        servicio.clasificarCartera(fechaNegocio);

        verify(cambioEstadoRepository, never()).save(any(CambioEstadoOperacion.class));
    }

    private OperacionCobranza crearOperacion(Integer id, String numero, String estado, BigDecimal saldo, int diasMora) {
        OperacionCobranza op = new OperacionCobranza();
        op.setIdOperacion(id);
        op.setIdSolicitud(id);
        op.setIdCliente(100 + id);
        op.setCedulaCliente("171000000" + id);
        op.setSistemaAmortizacion("FRANCES");
        op.setNumeroOperacion(numero);
        op.setMontoAprobado(saldo);
        op.setTeaAsignada(new BigDecimal("0.1550"));
        op.setPlazoMeses(12);
        op.setSeguroMensual(BigDecimal.ZERO);
        op.setContribucionSolca(BigDecimal.ZERO);
        op.setMontoNeto(saldo);
        op.setCuentaDestino("2200000001");
        op.setFechaDesembolso(LocalDate.of(2026, 1, 1));
        op.setEstadoInstruccionDesembolso("ENTREGADA");
        op.setEstadoCartera(estado);
        op.setSaldoCapital(saldo);
        op.setDiasMora(diasMora);
        return op;
    }

    private TramoCartera crearTramo(Integer id, int desde, Integer hasta, String estado, BigDecimal provision) {
        TramoCartera t = new TramoCartera();
        t.setIdTramo(id);
        t.setNumeroTramo(id);
        t.setDiasMoraDesde(desde);
        t.setDiasMoraHasta(hasta);
        t.setEstadoCartera(estado);
        t.setDevengaIngresos(id <= 3);
        t.setTratamientoMora("SE_CALCULA");
        t.setPorcentajeProvision(provision);
        t.setFechaVigenciaDesde(LocalDate.of(2026, 1, 1));
        return t;
    }
}
