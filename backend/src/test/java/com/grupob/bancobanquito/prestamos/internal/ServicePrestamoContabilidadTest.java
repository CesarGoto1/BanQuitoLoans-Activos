package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.amortizacion.CuotaCalculada;
import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.AsientoInvalidoException;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.originacion.ServiceSolicitudes;
import com.grupob.bancobanquito.originacion.SolicitudAprobada;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.pasivos.Cliente;
import com.grupob.bancobanquito.pasivos.Cuenta;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicePrestamoContabilidadTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);

    @Mock
    PrestamoRepository loanRepository;
    @Mock
    CuotaRepository installmentRepository;
    @Mock
    ServiceSolicitudes servicioSolicitudes;
    @Mock
    ServiceAmortizacion servicioAmortizacion;
    @Mock
    ServicePasivos servicioPasivos;
    @Mock
    ServiceParametro servicioParametro;
    @Mock
    ServiceFechaNegocio servicioFechaNegocio;
    @Mock
    ServiceContabilidad servicioContabilidad;
    @InjectMocks
    ServicePrestamoImpl servicioPrestamo;
    @Captor
    ArgumentCaptor<List<MovimientoContable>> movimientos;

    @BeforeEach
    void solicitudAprobada() {
        when(servicioSolicitudes.findApproved(9001)).thenReturn(Optional.of(new SolicitudAprobada(
                9001, 1, new BigDecimal("10000.00"), new BigDecimal("0.1550"), 1,
                MetodoAmortizacion.FRANCES)));
    }

    @Test
    void contabilizaNetoYSolcaAntesDeLasCuotasYDePasivos() {
        prepararDesembolso();
        prepararCuotas();

        var resultado = servicioPrestamo.disburse(9001, "2200000001");

        var orden = inOrder(loanRepository, servicioContabilidad, servicioAmortizacion,
                installmentRepository, servicioPasivos);
        orden.verify(loanRepository).save(any(Prestamo.class));
        orden.verify(servicioContabilidad).registrarAsientos(movimientos.capture());
        orden.verify(servicioAmortizacion).calculate(any(), any(), any(), any(), any());
        orden.verify(installmentRepository).saveAll(anyList());
        orden.verify(loanRepository).flush();
        orden.verify(servicioPasivos).creditDisbursement(any());
        assertThat(movimientos.getValue()).containsExactly(
                new MovimientoContable(42, FECHA, TipoAsiento.DESEMBOLSO,
                        CodigoCuentaContable.CARTERA_POR_VENCER, CodigoCuentaContable.CUENTA_CLIENTE,
                        new BigDecimal("9950.00"), "DESEMBOLSO-42-NETO"),
                new MovimientoContable(42, FECHA, TipoAsiento.DESEMBOLSO,
                        CodigoCuentaContable.CARTERA_POR_VENCER, CodigoCuentaContable.SOLCA_POR_PAGAR,
                        new BigDecimal("50.00"), "DESEMBOLSO-42-SOLCA"));
        assertThat(movimientos.getValue().stream().map(MovimientoContable::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10000.00");
        assertThat(resultado.montoNeto()).isEqualByComparingTo("9950.00");
        assertThat(resultado.numeroOperacion()).isEqualTo("OP-20261031-009001");
    }

    @Test
    void unReintentoDeUnaSolicitudDesembolsadaNoRepiteAsientosNiAcreditacion() {
        prepararDesembolso();
        prepararCuotas();
        when(loanRepository.existsByIdSolicitud(9001)).thenReturn(false, true);

        servicioPrestamo.disburse(9001, "2200000001");

        assertThatThrownBy(() -> servicioPrestamo.disburse(9001, "2200000001"))
                .isInstanceOf(PrestamoException.class).hasMessageContaining("ya fue desembolsada");
        verify(servicioContabilidad).registrarAsientos(anyList());
        verify(servicioPasivos).creditDisbursement(any());
        verify(loanRepository).save(any(Prestamo.class));
    }

    @Test
    void unFalloContableImpideGenerarCuotasYAcreditarEnPasivos() {
        prepararDesembolso();
        when(servicioContabilidad.registrarAsientos(anyList()))
                .thenThrow(new AsientoInvalidoException("Cuenta contable no disponible"));

        assertThatThrownBy(() -> servicioPrestamo.disburse(9001, "2200000001"))
                .isInstanceOf(AsientoInvalidoException.class);

        verifyNoInteractions(servicioAmortizacion, installmentRepository);
        verify(servicioPasivos, never()).creditDisbursement(any());
    }

    @Test
    void conservaElErrorCuandoFallaPasivos() {
        prepararDesembolso();
        prepararCuotas();
        doThrow(new IllegalStateException("Pasivos no disponible"))
                .when(servicioPasivos).creditDisbursement(any());

        assertThatThrownBy(() -> servicioPrestamo.disburse(9001, "2200000001"))
                .isInstanceOf(PrestamoException.class)
                .extracting(error -> ((PrestamoException) error).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_GATEWAY);
    }

    @Test
    void unaContribucionCeroNoGeneraUnAsientoDeMontoCero() {
        prepararDesembolso();
        prepararCuotas();
        when(servicioParametro.getValue("PAR-08")).thenReturn(BigDecimal.ZERO);

        servicioPrestamo.disburse(9001, "2200000001");

        verify(servicioContabilidad).registrarAsientos(movimientos.capture());
        assertThat(movimientos.getValue()).containsExactly(new MovimientoContable(42, FECHA,
                TipoAsiento.DESEMBOLSO, CodigoCuentaContable.CARTERA_POR_VENCER,
                CodigoCuentaContable.CUENTA_CLIENTE, new BigDecimal("10000.00"), "DESEMBOLSO-42-NETO"));
    }

    private void prepararDesembolso() {
        when(servicioPasivos.findCustomer(1)).thenReturn(Optional.of(
                new Cliente(1, "1710034065", "Pepito", "Perez")));
        when(servicioPasivos.findActiveAccounts(1)).thenReturn(List.of(new Cuenta("2200000001", "AHORROS")));
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(FECHA);
        when(servicioParametro.getValue("PAR-07")).thenReturn(new BigDecimal("0.0006"));
        when(servicioParametro.getValue("PAR-08")).thenReturn(new BigDecimal("0.0050"));
        when(loanRepository.save(any(Prestamo.class))).thenAnswer(invocacion -> {
            Prestamo loan = invocacion.getArgument(0);
            loan.setId(42);
            return loan;
        });
    }

    private void prepararCuotas() {
        when(servicioAmortizacion.calculate(any(), any(), any(), any(), any())).thenReturn(List.of(
                new CuotaCalculada(1, FECHA.plusMonths(1), new BigDecimal("10000.00"),
                        new BigDecimal("120.00"), BigDecimal.ZERO)));
    }
}
