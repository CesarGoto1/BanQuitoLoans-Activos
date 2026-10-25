package com.grupob.bancobanquito.amortization;

import java.math.BigDecimal;
import java.util.List;

public record TablaAmortizacion(
        BigDecimal tasaMensual,
        BigDecimal tasaNominalAnual,
        BigDecimal totalCapital,
        BigDecimal totalIntereses,
        BigDecimal totalSeguros,
        BigDecimal totalCuotas,
        List<CuotaAmortizacion> cuotas) {

    public TablaAmortizacion {
        cuotas = List.copyOf(cuotas);
    }
}
