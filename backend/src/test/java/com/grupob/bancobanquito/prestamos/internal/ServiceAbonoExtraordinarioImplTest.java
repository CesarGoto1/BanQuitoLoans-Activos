package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;
import com.grupob.bancobanquito.amortizacion.CuotaCalculada;
import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.ResultadoAbono;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig(ServiceAbonoExtraordinarioImplTest.Configuracion.class)
class ServiceAbonoExtraordinarioImplTest {

    private static final String OPERACION = "OP-20261031-000001";
    private static final String ANALISTA = "1700000001";
    private static final BigDecimal TEA = new BigDecimal("0.1677");
    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 10, 31);
    private static final LocalDate FECHA_ABONO = LocalDate.of(2026, 12, 15);

    @Configuration
    @ComponentScan("com.grupob.bancobanquito.amortizacion")
    static class Configuracion {
    }

    @Autowired
    ServiceAmortizacion servicioAmortizacion;

    private final PrestamoRepository prestamoRepository = mock(PrestamoRepository.class);
    private final CuotaRepository cuotaRepository = mock(CuotaRepository.class);
    private final PagoRepository pagoRepository = mock(PagoRepository.class);
    private final ServiceFechaNegocio servicioFechaNegocio = mock(ServiceFechaNegocio.class);
    private final ServiceParametro servicioParametro = mock(ServiceParametro.class);
    private final ServiceAnalista servicioAnalista = mock(ServiceAnalista.class);
    private final ServiceContabilidad servicioContabilidad = mock(ServiceContabilidad.class);

    private ServiceAbonoExtraordinarioImpl servicioAbono;
    private Prestamo prestamo;
    private List<Cuota> cuotas;

    @BeforeEach
    void preparar() {
        servicioAbono = new ServiceAbonoExtraordinarioImpl(prestamoRepository, cuotaRepository, pagoRepository,
                servicioAmortizacion, servicioFechaNegocio, servicioParametro, servicioAnalista,
                servicioContabilidad);
        prestamo = prestamoDelEjemplo();
        cuotas = cuotasDelEjemplo();
        cuotas.getFirst().setStatus(EstadoCuota.PAGADA);
        prestamo.setPrincipalBalance(new BigDecimal("9224.59"));

        when(prestamoRepository.findByNumeroOperacion(OPERACION)).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.findParaActualizar(OPERACION)).thenReturn(Optional.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(cuotas);
        when(pagoRepository.findUltimaFecha(any(), any())).thenReturn(Optional.empty());
        when(pagoRepository.save(any())).thenAnswer(invocacion -> {
            Pago pago = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pago, "id", 77);
            return pago;
        });
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(FECHA_ABONO);
        when(servicioParametro.getValue("PAR-11")).thenReturn(new BigDecimal("360.0000"));
        when(servicioParametro.getValue("PAR-12")).thenReturn(new BigDecimal("50.0000"));
        when(servicioAnalista.findByNationalId(ANALISTA))
                .thenReturn(Optional.of(new SesionAnalista(9001, ANALISTA, "Analista de pruebas")));
    }

    @Test
    void abonoDisminuyendoLaCuotaCoincideConElEjemplo() {
        ResultadoAbono resultado = servicioAbono.registrar(OPERACION, new BigDecimal("2000.00"),
                ModalidadAbono.DISMINUIR_CUOTA, ANALISTA);

        assertThat(resultado.interesLiquidado()).isEqualByComparingTo("59.98");
        assertThat(resultado.capitalAbonado()).isEqualByComparingTo("1940.02");
        assertThat(resultado.saldoNuevo()).isEqualByComparingTo("7284.57");
        assertThat(resultado.antes().numeroCuotas()).isEqualTo(11);
        assertThat(resultado.antes().valorCuota()).isEqualByComparingTo("911.44");
        assertThat(resultado.despues().numeroCuotas()).isEqualTo(11);
        assertThat(resultado.despues().valorCuota()).isEqualByComparingTo("716.73");
        assertThat(resultado.despues().cuotas().getLast().valorCuota()).isEqualByComparingTo("716.74");
        assertThat(resultado.despues().fechaFinal()).isEqualTo(LocalDate.of(2027, 10, 31));

        assertThat(prestamo.getPrincipalBalance()).isEqualByComparingTo("7284.57");
        assertThat(cuotas.get(1).getInterestAmount()).isEqualByComparingTo("50.52");
        assertThat(cuotas.getFirst().getPrincipalAmount()).isEqualByComparingTo("775.41");
        verify(cuotaRepository).deleteAll(List.of());
    }

    @Test
    void abonoDisminuyendoElPlazoEliminaLasUltimasCuotas() {
        ResultadoAbono resultado = servicioAbono.registrar(OPERACION, new BigDecimal("2000.00"),
                ModalidadAbono.DISMINUIR_PLAZO, ANALISTA);

        assertThat(resultado.despues().numeroCuotas()).isEqualTo(9);
        assertThat(resultado.despues().valorCuota()).isEqualByComparingTo("911.44");
        assertThat(resultado.despues().cuotas().getLast().valorCuota()).isEqualByComparingTo("459.31");
        assertThat(resultado.despues().fechaFinal()).isEqualTo(LocalDate.of(2027, 8, 31));
        verify(cuotaRepository).deleteAll(List.of(cuotas.get(10), cuotas.get(11)));
    }

    @Test
    void guardaElPagoYLosAsientosQueCuadran() {
        servicioAbono.registrar(OPERACION, new BigDecimal("2000.00"), ModalidadAbono.DISMINUIR_CUOTA, ANALISTA);

        ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(pago.capture());
        assertThat(pago.getValue().getOrigen()).isEqualTo(OrigenPago.ABONO_EXTRAORDINARIO);
        assertThat(pago.getValue().getModalidadAbono()).isEqualTo(ModalidadAbono.DISMINUIR_CUOTA);
        assertThat(pago.getValue().getMontoRecibido()).isEqualByComparingTo("2000.00");
        DetallePago detalle = pago.getValue().getDetalles().getFirst();
        assertThat(detalle.getIdCuota()).isNull();
        assertThat(detalle.getAplicadoInteres()).isEqualByComparingTo("59.98");
        assertThat(detalle.getAplicadoCapital()).isEqualByComparingTo("1940.02");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MovimientoContable>> asientos = ArgumentCaptor.forClass(List.class);
        verify(servicioContabilidad).registrarAsientos(asientos.capture());
        assertThat(asientos.getValue()).extracting(MovimientoContable::cuentaCredito)
                .containsExactly(CodigoCuentaContable.CARTERA_POR_VENCER, CodigoCuentaContable.INTERESES_POR_COBRAR);
        assertThat(asientos.getValue().stream().map(MovimientoContable::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("2000.00");
        assertThat(asientos.getValue()).extracting(MovimientoContable::claveIdempotencia)
                .containsExactly("ABONO-77-CAPITAL", "ABONO-77-INTERES");
    }

    @Test
    void simularNoGuardaNada() {
        ResultadoAbono resultado = servicioAbono.simular(OPERACION, new BigDecimal("2000.00"),
                ModalidadAbono.DISMINUIR_CUOTA);

        assertThat(resultado.despues().valorCuota()).isEqualByComparingTo("716.73");
        assertThat(prestamo.getPrincipalBalance()).isEqualByComparingTo("9224.59");
        verify(pagoRepository, never()).save(any());
        verify(servicioContabilidad, never()).registrarAsientos(anyList());
    }

    @Test
    void rechazaUnaOperacionConCuotasVencidas() {
        cuotas.getFirst().setStatus(EstadoCuota.PENDIENTE);

        assertThatThrownBy(() -> servicioAbono.simular(OPERACION, new BigDecimal("2000.00"),
                ModalidadAbono.DISMINUIR_CUOTA))
                .isInstanceOf(PrestamoException.class)
                .hasMessageContaining("cuotas vencidas");
    }

    @Test
    void rechazaUnaOperacionVencida() {
        prestamo.setPortfolioStatus(EstadoCartera.VENCIDA);

        assertThatThrownBy(() -> servicioAbono.simular(OPERACION, new BigDecimal("2000.00"),
                ModalidadAbono.DISMINUIR_CUOTA))
                .hasMessageContaining("prelación");
    }

    @Test
    void rechazaUnAbonoMenorAlMinimo() {
        assertThatThrownBy(() -> servicioAbono.simular(OPERACION, new BigDecimal("49.99"),
                ModalidadAbono.DISMINUIR_CUOTA))
                .hasMessageContaining("mínimo es USD 50.00");
    }

    @Test
    void rechazaUnAbonoQueNoSuperaElInteres() {
        assertThatThrownBy(() -> servicioAbono.simular(OPERACION, new BigDecimal("59.98"),
                ModalidadAbono.DISMINUIR_CUOTA))
                .hasMessageContaining("mayor al interés devengado");
    }

    @Test
    void rechazaUnAbonoQueCubreTodaLaDeuda() {
        assertThatThrownBy(() -> servicioAbono.simular(OPERACION, new BigDecimal("9284.57"),
                ModalidadAbono.DISMINUIR_CUOTA))
                .hasMessageContaining("cancelación anticipada");
    }

    @Test
    void rechazaUnSegundoAbonoElMismoDia() {
        when(pagoRepository.existsByIdOperacionAndOrigenAndFechaPago(1, OrigenPago.ABONO_EXTRAORDINARIO,
                FECHA_ABONO)).thenReturn(true);

        assertThatThrownBy(() -> servicioAbono.registrar(OPERACION, new BigDecimal("2000.00"),
                ModalidadAbono.DISMINUIR_CUOTA, ANALISTA))
                .hasMessageContaining("ya tiene un abono");
    }

    @Test
    void rechazaMontosConMasDeDosDecimales() {
        assertThatThrownBy(() -> servicioAbono.simular(OPERACION, new BigDecimal("100.001"),
                ModalidadAbono.DISMINUIR_CUOTA))
                .hasMessageContaining("dos decimales");
    }

    private Prestamo prestamoDelEjemplo() {
        Prestamo nuevo = new Prestamo(1);
        nuevo.setLoanNumber(OPERACION);
        nuevo.setEffectiveAnnualRate(TEA);
        nuevo.setAmortizationMethod(MetodoAmortizacion.FRANCES);
        nuevo.setDisbursementDate(DESEMBOLSO);
        nuevo.setMonthlyInsurance(new BigDecimal("6.00"));
        nuevo.setPortfolioStatus(EstadoCartera.POR_VENCER);
        return nuevo;
    }

    private List<Cuota> cuotasDelEjemplo() {
        List<Cuota> lista = new ArrayList<>();
        for (CuotaCalculada calculada : servicioAmortizacion.calculate(new BigDecimal("10000.00"), TEA, 12,
                MetodoAmortizacion.FRANCES, DESEMBOLSO)) {
            Cuota cuota = new Cuota(calculada.numeroCuota());
            cuota.setInstallmentNumber(calculada.numeroCuota());
            cuota.setDueDate(calculada.fechaVencimiento());
            cuota.setPrincipalAmount(calculada.capital());
            cuota.setInterestAmount(calculada.interes());
            cuota.setInsuranceAmount(new BigDecimal("6.00"));
            cuota.setRemainingPrincipalBalance(calculada.remainingPrincipalBalance());
            cuota.setStatus(EstadoCuota.PENDIENTE);
            lista.add(cuota);
        }
        return lista;
    }
}
