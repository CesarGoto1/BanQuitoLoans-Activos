package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;

public record ImputacionCuota(
        Integer idCuota,
        BigDecimal seguro,
        BigDecimal mora,
        BigDecimal interes,
        BigDecimal capital) {
}
