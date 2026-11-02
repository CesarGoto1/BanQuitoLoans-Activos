package com.grupob.bancobanquito.amortizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecalculoTabla(
        BigDecimal saldoCapital,
        BigDecimal teaAsignada,
        MetodoAmortizacion metodo,
        ModalidadAbono modalidad,
        LocalDate fechaAbono,
        Integer primeraCuota,
        List<LocalDate> vencimientosPendientes,
        BigDecimal cuotaVigente,
        Integer baseDias) {
}
