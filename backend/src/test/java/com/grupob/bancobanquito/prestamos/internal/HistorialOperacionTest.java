package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import com.grupob.bancobanquito.prestamos.CargoMora;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.HistorialOperacion;
import com.grupob.bancobanquito.prestamos.PagoAplicado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class HistorialOperacionTest {

    private static final String OPERACION = "OP-20261031-000001";

    private final PrestamoRepository prestamoRepository = mock(PrestamoRepository.class);
    private final CuotaRepository cuotaRepository = mock(CuotaRepository.class);
    private final PagoRepository pagoRepository = mock(PagoRepository.class);
    private final CambioEstadoOperacionRepository cambioEstadoRepository = mock(CambioEstadoOperacionRepository.class);
    private final ConsultaPrestamoServiceImpl servicio = new ConsultaPrestamoServiceImpl(prestamoRepository,
            cuotaRepository, pagoRepository, cambioEstadoRepository);

    private Cuota primera;
    private Cuota segunda;

    @BeforeEach
    void preparar() {
        Prestamo prestamo = new Prestamo(1);
        prestamo.setLoanNumber(OPERACION);
        primera = cuota(10, 1, LocalDate.of(2026, 11, 30));
        segunda = cuota(11, 2, LocalDate.of(2026, 12, 31));
        when(prestamoRepository.findByNumeroOperacion(OPERACION)).thenReturn(Optional.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(List.of(primera, segunda));
        when(pagoRepository.findByIdOperacionOrderByFechaPagoAscIdAsc(1)).thenReturn(List.of());
        when(cambioEstadoRepository.findByIdOperacionOrderByFechaCambioAscIdAsc(1)).thenReturn(List.of());
    }

    @Test
    void muestraCadaPagoConSuDesgloseYElNumeroDeCuota() {
        Pago abono = new Pago(1, 7, OrigenPago.ABONO_EXTRAORDINARIO, LocalDate.of(2026, 12, 15),
                new BigDecimal("2000.00"), ModalidadAbono.DISMINUIR_CUOTA, "1700000001");
        abono.agregarDetalle(null, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("59.98"), new BigDecimal("1940.02"));
        ReflectionTestUtils.setField(abono, "id", 5);
        Pago cancelacion = new Pago(1, 7, OrigenPago.CANCELACION_ANTICIPADA, LocalDate.of(2027, 1, 10),
                new BigDecimal("710.73"), null, "1700000001");
        cancelacion.agregarDetalle(10, new BigDecimal("6.00"), new BigDecimal("1.25"), new BigDecimal("50.00"),
                new BigDecimal("600.00"));
        cancelacion.agregarDetalle(11, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("53.48"));
        when(pagoRepository.findByIdOperacionOrderByFechaPagoAscIdAsc(1)).thenReturn(List.of(abono, cancelacion));

        HistorialOperacion historial = servicio.buscarHistorial(OPERACION);

        assertThat(historial.pagos()).hasSize(2);
        PagoAplicado primerPago = historial.pagos().getFirst();
        assertThat(primerPago.idPago()).isEqualTo(5);
        assertThat(primerPago.tipoPago()).isEqualTo("ABONO_EXTRAORDINARIO");
        assertThat(primerPago.modalidadAbono()).isEqualTo("DISMINUIR_CUOTA");
        assertThat(primerPago.aplicadoInteres()).isEqualByComparingTo("59.98");
        assertThat(primerPago.aplicadoCapital()).isEqualByComparingTo("1940.02");
        assertThat(primerPago.detalles().getFirst().numeroCuota()).isNull();
        assertThat(primerPago.registradoPor()).isEqualTo("1700000001");

        PagoAplicado segundoPago = historial.pagos().get(1);
        assertThat(segundoPago.modalidadAbono()).isNull();
        assertThat(segundoPago.detalles()).extracting("numeroCuota").containsExactly(1, 2);
        assertThat(segundoPago.aplicadoSeguro()).isEqualByComparingTo("6.00");
        assertThat(segundoPago.aplicadoMora()).isEqualByComparingTo("1.25");
        assertThat(segundoPago.aplicadoCapital()).isEqualByComparingTo("653.48");
    }

    @Test
    void listaSoloLasCuotasConMoraYLoQueFaltaPagar() {
        primera.setAccruedDefaultInterest(new BigDecimal("3.3333"));
        primera.setDefaultInterestPaid(new BigDecimal("1.00"));
        primera.setStatus(EstadoCuota.PAGO_PARCIAL);

        List<CargoMora> cargos = servicio.buscarHistorial(OPERACION).cargosMora();

        assertThat(cargos).hasSize(1);
        assertThat(cargos.getFirst().numeroCuota()).isEqualTo(1);
        assertThat(cargos.getFirst().moraGenerada()).isEqualByComparingTo("3.33");
        assertThat(cargos.getFirst().moraPagada()).isEqualByComparingTo("1.00");
        assertThat(cargos.getFirst().moraPendiente()).isEqualByComparingTo("2.33");
    }

    @Test
    void muestraLosMovimientosDeEstadoEnOrden() {
        when(cambioEstadoRepository.findByIdOperacionOrderByFechaCambioAscIdAsc(1)).thenReturn(List.of(
                new CambioEstadoOperacion(1, LocalDate.of(2026, 12, 1), EstadoCartera.POR_VENCER,
                        EstadoCartera.VENCIDA, 1, "sistema"),
                new CambioEstadoOperacion(1, LocalDate.of(2026, 12, 15), EstadoCartera.VENCIDA,
                        EstadoCartera.CANCELADA, 0, "1700000001")));

        HistorialOperacion historial = servicio.buscarHistorial(OPERACION);

        assertThat(historial.movimientosEstado()).extracting("estadoNuevo")
                .containsExactly(EstadoCartera.VENCIDA, EstadoCartera.CANCELADA);
        assertThat(historial.movimientosEstado().getFirst().diasMora()).isEqualTo(1);
        assertThat(historial.pagos()).isEmpty();
        assertThat(historial.cargosMora()).isEmpty();
    }

    @Test
    void respondeNoEncontradoSiLaOperacionNoExiste() {
        assertThatThrownBy(() -> servicio.buscarHistorial("OP-20261031-999999"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Cuota cuota(int id, int numero, LocalDate vencimiento) {
        Cuota cuota = new Cuota(id);
        cuota.setInstallmentNumber(numero);
        cuota.setDueDate(vencimiento);
        cuota.setAccruedDefaultInterest(new BigDecimal("0.0000"));
        cuota.setDefaultInterestPaid(BigDecimal.ZERO);
        cuota.setStatus(EstadoCuota.PENDIENTE);
        return cuota;
    }
}
