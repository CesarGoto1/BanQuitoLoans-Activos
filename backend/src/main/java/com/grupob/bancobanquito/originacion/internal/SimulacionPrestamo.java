package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;
import java.util.List;

record SimulacionPrestamo(
        BigDecimal monto,
        Integer plazoMeses,
        MetodoAmortizacion metodoAmortizacion,
        BigDecimal teaAsignada,
        BigDecimal tasaNominalAnual,
        BigDecimal seguroMensual,
        BigDecimal contribucionSolca,
        BigDecimal montoNeto,
        BigDecimal primeraCuota,
        BigDecimal ultimaCuota,
        BigDecimal interesTotal,
        BigDecimal seguroTotal,
        BigDecimal costoTotal,
        List<CuotaSimulada> cuotas) {

    BigDecimal highestInstallment() {
        return cuotas.stream().map(CuotaSimulada::total).max(BigDecimal::compareTo).orElseThrow();
    }
}
