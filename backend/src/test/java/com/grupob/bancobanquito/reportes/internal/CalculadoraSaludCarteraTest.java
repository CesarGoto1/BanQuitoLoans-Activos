package com.grupob.bancobanquito.reportes.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static com.grupob.bancobanquito.reportes.internal.ServiceReportesCarteraTest.operacion;
import static com.grupob.bancobanquito.reportes.internal.ServiceReportesCarteraTest.tramo;
import com.grupob.bancobanquito.reportes.*;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraSaludCarteraTest {
    private final ServiceReportesCarteraImpl servicio = new ServiceReportesCarteraImpl(org.mockito.Mockito.mock(
            com.grupob.bancobanquito.cobranza.ServicioFotografiaCartera.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.cierre.ServiceCierreDia.class),
            org.mockito.Mockito.mock(com.grupob.bancobanquito.prestamos.ConsultaPrestamoService.class));

    @Test
    void distribuyePorMontoOriginalYPlazoPeroPonderaPorSaldo() {
        var corte = new CorteCartera(LocalDate.of(2026, 10, 31), List.of(
                operacion(1, 1, EstadoCartera.POR_VENCER, "100.00", "10.00"),
                operacion(2, 2, EstadoCartera.VENCIDA, "300.00", "30.00"),
                operacion(3, 3, EstadoCartera.CASTIGADA, "900.00", "900.00")), List.of(
                tramo(1, 1, EstadoCartera.POR_VENCER), tramo(2, 2, EstadoCartera.VENCIDA),
                tramo(3, 3, EstadoCartera.CASTIGADA)));
        var resultado = servicio.calcularSalud(corte, List.of(
                new DatosOperacionReporte(1, 1, new BigDecimal("2000.50"), 13, MetodoAmortizacion.FRANCES),
                new DatosOperacionReporte(2, 2, new BigDecimal("10000.01"), 37, MetodoAmortizacion.ALEMAN)));
        assertThat(resultado.provisionesCarteraBruta()).isEqualByComparingTo("40.00");
        assertThat(resultado.coberturaProvisiones()).isEqualByComparingTo("13.33");
        assertThat(resultado.porMontoOriginal().get(1).saldoCapital()).isEqualByComparingTo("100.00");
        assertThat(resultado.porMontoOriginal().get(2).participacion()).isEqualByComparingTo("75.00");
        assertThat(resultado.porPlazo().get(1).cantidadOperaciones()).isEqualTo(1);
        assertThat(resultado.porPlazo().get(2).participacion()).isEqualByComparingTo("75.00");
        assertThat(resultado.porSistemaAmortizacion().getFirst().participacion()).isEqualByComparingTo("25.00");
    }

    @Test
    void devuelveNullEnTodosLosPorcentajesSinDenominador() {
        var resultado = servicio.calcularSalud(new CorteCartera(LocalDate.of(2026, 10, 31),
                List.of(), List.of()), List.of());
        assertThat(resultado.coberturaProvisiones()).isNull();
        assertThat(resultado.porMontoOriginal()).allSatisfy(c -> assertThat(c.participacion()).isNull());
        assertThat(resultado.porPlazo()).allSatisfy(c -> assertThat(c.participacion()).isNull());
        assertThat(resultado.porSistemaAmortizacion()).allSatisfy(c -> assertThat(c.participacion()).isNull());
    }
}
