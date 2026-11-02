package com.grupob.bancobanquito.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoCuadreContable(
        LocalDate fechaNegocio,
        BigDecimal totalDebitos,
        BigDecimal totalCreditos,
        BigDecimal diferencia,
        long cantidadAsientos,
        boolean cuadrado,
        String observacion) {
}
