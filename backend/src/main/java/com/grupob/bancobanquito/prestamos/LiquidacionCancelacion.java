package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidacionCancelacion(
        String numeroOperacion,
        LocalDate fechaCancelacion,
        EstadoCartera estadoCartera,
        BigDecimal capitalInsoluto,
        BigDecimal interesCuotaEnCurso,
        BigDecimal interesVencido,
        BigDecimal seguroVencido,
        BigDecimal moraVencida,
        BigDecimal valorCancelacion) {
}
