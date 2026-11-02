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
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.LiquidacionCancelacion;
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

@SpringJUnitConfig(ServiceCancelacionAnticipadaImplTest.Configuracion.class)
class ServiceCancelacionAnticipadaImplTest {

    private static final String OPERACION = "OP-20261031-000001";
    private static final String ANALISTA = "1700000001";
    private static final BigDecimal TEA = new BigDecimal("0.1677");
    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 10, 31);

    @Configuration
    @ComponentScan("com.grupob.bancobanquito.amortizacion")
    static class Configuracion {
    }

    @Autowired
    ServiceAmortizacion servicioAmortizacion;

    private final PrestamoRepository prestamoRepository = mock(PrestamoRepository.class);
    private final CuotaRepository cuotaRepository = mock(CuotaRepository.class);
    private final PagoRepository pagoRepository = mock(PagoRepository.class);
    private final CambioEstadoOperacionRepository cambioEstadoRepository = mock(CambioEstadoOperacionRepository.class);
    private final ServiceFechaNegocio servicioFechaNegocio = mock(ServiceFechaNegocio.class);
    private final ServiceParametro servicioParametro = mock(ServiceParametro.class);
    private final ServiceAnalista servicioAnalista = mock(ServiceAnalista.class);
    private final ServiceContabilidad servicioContabilidad = mock(ServiceContabilidad.class);

    private ServiceCancelacionAnticipadaImpl servicioCancelacion;
    private Prestamo prestamo;
    private List<Cuota> cuotas;

    @BeforeEach
    void preparar() {
        servicioCancelacion = new ServiceCancelacionAnticipadaImpl(prestamoRepository, cuotaRepository,
                pagoRepository, cambioEstadoRepository, servicioAmortizacion, servicioFechaNegocio,
                servicioParametro, servicioAnalista, servicioContabilidad);
        prestamo = prestamoDelEjemplo();
        cuotas = cuotasDelEjemplo();

        when(prestamoRepository.findByNumeroOperacion(OPERACION)).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.findParaActualizar(OPERACION)).thenReturn(Optional.of(prestamo));
        when(cuotaRepository.findByLoanIdOrderByNumeroCuotaAsc(1)).thenReturn(cuotas);
        when(pagoRepository.findUltimaFecha(any(), any())).thenReturn(Optional.empty());
        when(pagoRepository.save(any())).thenAnswer(invocacion -> {
            Pago pago = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pago, "id", 88);
            return pago;
        });
        when(servicioParametro.getValue("PAR-11")).thenReturn(new BigDecimal("360.0000"));
        when(servicioAnalista.findByNationalId(ANALISTA))
                .thenReturn(Optional.of(new SesionAnalista(9001, ANALISTA, "Analista de pruebas")));
    }

    @Test
    void valorDeCancelacionAlDiaConLaCuotaUnoPagada() {
        pagarPrimeraCuota();
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(LocalDate.of(2026, 12, 15));

        LiquidacionCancelacion liquidacion = servicioCancelacion.consultar(OPERACION);

        assertThat(liquidacion.capitalInsoluto()).isEqualByComparingTo("9224.59");
        assertThat(liquidacion.interesCuotaEnCurso()).isEqualByComparingTo("59.98");
        assertThat(liquidacion.interesVencido()).isZero();
        assertThat(liquidacion.seguroVencido()).isZero();
        assertThat(liquidacion.moraVencida()).isZero();
        assertThat(liquidacion.valorCancelacion()).isEqualByComparingTo("9284.57");
    }

    @Test
    void valorDeCancelacionConLaCuotaUnoVencida() {
        prestamo.setPortfolioStatus(EstadoCartera.VENCIDA);
        prestamo.setDaysPastDue(10);
        cuotas.getFirst().setAccruedDefaultInterest(new BigDecimal("3.3300"));
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(LocalDate.of(2026, 12, 10));

        LiquidacionCancelacion liquidacion = servicioCancelacion.consultar(OPERACION);

        assertThat(liquidacion.capitalInsoluto()).isEqualByComparingTo("10000.00");
        assertThat(liquidacion.interesCuotaEnCurso()).isEqualByComparingTo("39.98");
        assertThat(liquidacion.interesVencido()).isEqualByComparingTo("130.03");
        assertThat(liquidacion.seguroVencido()).isEqualByComparingTo("6.00");
        assertThat(liquidacion.moraVencida()).isEqualByComparingTo("3.33");
        assertThat(liquidacion.valorCancelacion()).isEqualByComparingTo("10179.34");
    }

    @Test
    void registrarCancelaLaOperacionYCierraLaTabla() {
        pagarPrimeraCuota();
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(LocalDate.of(2026, 12, 15));

        servicioCancelacion.registrar(OPERACION, new BigDecimal("9284.57"), ANALISTA);

        assertThat(prestamo.getPortfolioStatus()).isEqualTo(EstadoCartera.CANCELADA);
        assertThat(prestamo.getPrincipalBalance()).isZero();
        assertThat(cuotas).allSatisfy(cuota -> {
            assertThat(cuota.getStatus()).isEqualTo(EstadoCuota.PAGADA);
            assertThat(cuota.getPrincipalPaid()).isEqualByComparingTo(cuota.getPrincipalAmount());
            assertThat(cuota.getInterestPaid()).isEqualByComparingTo(cuota.getInterestAmount());
            assertThat(cuota.getInsurancePaid()).isEqualByComparingTo(cuota.getInsuranceAmount());
        });
        assertThat(cuotas.get(1).getInterestAmount()).isEqualByComparingTo("59.98");
        assertThat(cuotas.get(1).getInsuranceAmount()).isZero();
        assertThat(cuotas.get(2).getInterestAmount()).isZero();
        assertThat(cuotas.stream().map(Cuota::getPrincipalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("10000.00");

        ArgumentCaptor<CambioEstadoOperacion> cambio = ArgumentCaptor.forClass(CambioEstadoOperacion.class);
        verify(cambioEstadoRepository).save(cambio.capture());
        assertThat(cambio.getValue().getEstadoAnterior()).isEqualTo(EstadoCartera.POR_VENCER);
        assertThat(cambio.getValue().getEstadoNuevo()).isEqualTo(EstadoCartera.CANCELADA);

        ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(pago.capture());
        assertThat(pago.getValue().getOrigen()).isEqualTo(OrigenPago.CANCELACION_ANTICIPADA);
        assertThat(pago.getValue().getDetalles()).hasSize(11);
    }

    @Test
    void losAsientosSumanElValorCobrado() {
        prestamo.setPortfolioStatus(EstadoCartera.VENCIDA);
        cuotas.getFirst().setAccruedDefaultInterest(new BigDecimal("3.3300"));
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(LocalDate.of(2026, 12, 10));

        servicioCancelacion.registrar(OPERACION, new BigDecimal("10179.34"), ANALISTA);

        List<MovimientoContable> asientos = asientosRegistrados();
        assertThat(asientos).extracting(MovimientoContable::cuentaCredito).containsExactly(
                CodigoCuentaContable.CARTERA_VENCIDA, CodigoCuentaContable.INTERESES_POR_COBRAR,
                CodigoCuentaContable.INTERESES_MORA_POR_COBRAR, CodigoCuentaContable.SEGUROS_POR_PAGAR);
        assertThat(total(asientos)).isEqualByComparingTo("10179.34");
        assertThat(cuotas.getFirst().getDefaultInterestPaid()).isEqualByComparingTo("3.33");
    }

    @Test
    void unaOperacionCastigadaSeRegistraComoRecuperacion() {
        prestamo.setPortfolioStatus(EstadoCartera.CASTIGADA);
        cuotas.getFirst().setAccruedDefaultInterest(new BigDecimal("3.3300"));
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(LocalDate.of(2026, 12, 10));

        servicioCancelacion.registrar(OPERACION, new BigDecimal("10179.34"), ANALISTA);

        List<MovimientoContable> asientos = asientosRegistrados();
        assertThat(asientos).extracting(MovimientoContable::tipoAsiento).containsOnly(TipoAsiento.RECUPERACION);
        assertThat(asientos).extracting(MovimientoContable::cuentaCredito).containsExactly(
                CodigoCuentaContable.INGRESOS_RECUPERACIONES, CodigoCuentaContable.CARTERA_CASTIGADA,
                CodigoCuentaContable.SEGUROS_POR_PAGAR);
        assertThat(asientos.getFirst().monto()).isEqualByComparingTo("10173.34");
    }

    @Test
    void rechazaUnValorDistintoAlDeCancelacion() {
        pagarPrimeraCuota();
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(LocalDate.of(2026, 12, 15));

        assertThatThrownBy(() -> servicioCancelacion.registrar(OPERACION, new BigDecimal("9000.00"), ANALISTA))
                .isInstanceOf(PrestamoException.class)
                .hasMessageContaining("USD 9284.57");
        verify(pagoRepository, never()).save(any());
        verify(servicioContabilidad, never()).registrarAsientos(anyList());
    }

    @Test
    void rechazaUnaOperacionYaCancelada() {
        prestamo.setPortfolioStatus(EstadoCartera.CANCELADA);

        assertThatThrownBy(() -> servicioCancelacion.consultar(OPERACION))
                .hasMessageContaining("ya está cancelada");
    }

    @Test
    void rechazaUnAnalistaNoRegistrado() {
        assertThatThrownBy(() -> servicioCancelacion.registrar(OPERACION, BigDecimal.ONE, "0999999999"))
                .hasMessageContaining("no está registrado");
    }

    private List<MovimientoContable> asientosRegistrados() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MovimientoContable>> asientos = ArgumentCaptor.forClass(List.class);
        verify(servicioContabilidad).registrarAsientos(asientos.capture());
        return asientos.getValue();
    }

    private BigDecimal total(List<MovimientoContable> asientos) {
        return asientos.stream()
                .filter(asiento -> asiento.cuentaDebito() == CodigoCuentaContable.RECAUDACIONES_POR_APLICAR)
                .map(MovimientoContable::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void pagarPrimeraCuota() {
        Cuota primera = cuotas.getFirst();
        primera.setPrincipalPaid(primera.getPrincipalAmount());
        primera.setInterestPaid(primera.getInterestAmount());
        primera.setInsurancePaid(primera.getInsuranceAmount());
        primera.setStatus(EstadoCuota.PAGADA);
        prestamo.setPrincipalBalance(new BigDecimal("9224.59"));
    }

    private Prestamo prestamoDelEjemplo() {
        Prestamo nuevo = new Prestamo(1);
        nuevo.setLoanNumber(OPERACION);
        nuevo.setEffectiveAnnualRate(TEA);
        nuevo.setAmortizationMethod(MetodoAmortizacion.FRANCES);
        nuevo.setDisbursementDate(DESEMBOLSO);
        nuevo.setMonthlyInsurance(new BigDecimal("6.00"));
        nuevo.setPortfolioStatus(EstadoCartera.POR_VENCER);
        nuevo.setPrincipalBalance(new BigDecimal("10000.00"));
        nuevo.setDaysPastDue(0);
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
            cuota.setInsurancePaid(BigDecimal.ZERO);
            cuota.setDefaultInterestPaid(BigDecimal.ZERO);
            cuota.setInterestPaid(BigDecimal.ZERO);
            cuota.setPrincipalPaid(BigDecimal.ZERO);
            cuota.setAccruedDefaultInterest(BigDecimal.ZERO);
            cuota.setStatus(EstadoCuota.PENDIENTE);
            lista.add(cuota);
        }
        return lista;
    }
}
