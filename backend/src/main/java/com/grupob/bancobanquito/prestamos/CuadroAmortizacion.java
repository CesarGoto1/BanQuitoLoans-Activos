package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CuadroAmortizacion(
        List<ResumenCuota> cuotas,
        Integer numeroCuotas,
        BigDecimal valorCuota,
        BigDecimal totalInteres,
        LocalDate fechaFinal) {
}
