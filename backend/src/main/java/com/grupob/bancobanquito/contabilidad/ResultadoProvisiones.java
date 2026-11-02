package com.grupob.bancobanquito.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoProvisiones(
        LocalDate fechaAsiento,
        int operacionesProcesadas,
        int asientosRegistrados,
        BigDecimal provisionTotal,
        BigDecimal variacionTotal) {
}
