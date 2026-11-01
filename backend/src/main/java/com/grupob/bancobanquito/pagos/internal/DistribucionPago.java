package com.grupob.bancobanquito.pagos.internal;

import com.grupob.bancobanquito.prestamos.ImputacionCuota;
import java.math.BigDecimal;
import java.util.List;

record DistribucionPago(
        List<ImputacionCuota> imputaciones,
        BigDecimal seguro,
        BigDecimal mora,
        BigDecimal interes,
        BigDecimal capital,
        BigDecimal anticipado,
        BigDecimal excedente,
        boolean cubreTodaLaDeuda) {

    BigDecimal totalAplicado() {
        return seguro.add(mora).add(interes).add(capital).add(anticipado);
    }
}
