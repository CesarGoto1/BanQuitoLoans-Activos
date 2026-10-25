package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.amortization.SistemaAmortizacion;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulacionServiceTest {
    private final ParameterService parametros = mock(ParameterService.class);
    private final BusinessDateService fechas = mock(BusinessDateService.class);
    private final LocalDate fecha = LocalDate.of(2026, 10, 31);
    private SimulacionService servicio;

    @BeforeEach
    void preparar() {
        when(fechas.getCurrentBusinessDate()).thenReturn(fecha);
        Map<String, String> valores = Map.of("PAR-01", "500", "PAR-02", "30000", "PAR-03", "6", "PAR-04", "60",
                "PAR-05", "0.1677", "PAR-06", "0.1550", "PAR-07", "0.0006", "PAR-08", "0.0050");
        valores.forEach((codigo, valor) -> when(parametros.getValue(codigo, fecha)).thenReturn(new BigDecimal(valor)));
        servicio = new SimulacionServiceImpl(parametros, fechas, new AmortizacionServiceImpl());
    }

    @Test
    void usarTasaReferencialYFechaDeNegocioSinConfundirEjemplo86() {
        var respuesta = servicio.simular(new SimulacionRequest(new BigDecimal("10000"), 12, SistemaAmortizacion.FRANCES));
        assertThat(respuesta.teaReferencial()).isEqualByComparingTo("0.1550");
        assertThat(respuesta.fechaNegocio()).isEqualTo(fecha);
        assertThat(respuesta.seguroMensual()).isEqualByComparingTo("6.00");
        assertThat(respuesta.contribucionSolca()).isEqualByComparingTo("50.00");
        assertThat(respuesta.montoNeto()).isEqualByComparingTo("9950.00");
        assertThat(respuesta.primeraCuota()).isNotEqualByComparingTo("911.44");
        assertThat(respuesta.costoTotalCredito()).isEqualByComparingTo(respuesta.tablaAmortizacion().totalCuotas().subtract(respuesta.montoNeto()));
        verify(fechas, times(1)).getCurrentBusinessDate();
    }

    @Test
    void reproducirEjemploAlConfigurarSuTasa() {
        when(parametros.getValue("PAR-06", fecha)).thenReturn(new BigDecimal("0.1677"));
        var respuesta = servicio.simular(new SimulacionRequest(new BigDecimal("10000"), 12, SistemaAmortizacion.FRANCES));
        assertThat(respuesta.primeraCuota()).isEqualByComparingTo("911.44");
        assertThat(respuesta.costoTotalCredito()).isEqualByComparingTo("987.23");
    }

    @ParameterizedTest
    @CsvSource({"499.99,12", "30000.01,12", "10000,5", "10000,61"})
    void rechazarCondicionesFueraDeParametros(BigDecimal monto, int plazo) {
        assertThatThrownBy(() -> servicio.simular(new SimulacionRequest(monto, plazo, SistemaAmortizacion.FRANCES)))
                .isInstanceOf(CondicionesSimulacionException.class);
    }

    @ParameterizedTest
    @CsvSource({"500,6", "30000,60"})
    void aceptarLimitesInclusivos(BigDecimal monto, int plazo) {
        assertThat(servicio.simular(new SimulacionRequest(monto, plazo, SistemaAmortizacion.ALEMAN))
                .tablaAmortizacion().cuotas()).hasSize(plazo);
    }

    @Test
    void rechazarParametrosIncoherentes() {
        when(parametros.getValue("PAR-06", fecha)).thenReturn(new BigDecimal("0.18"));
        assertThatThrownBy(() -> servicio.consultarParametros()).isInstanceOf(IllegalStateException.class);
    }
}
