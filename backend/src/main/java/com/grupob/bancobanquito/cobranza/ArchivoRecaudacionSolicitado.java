package com.grupob.bancobanquito.cobranza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArchivoRecaudacionSolicitado(LocalDate fechaProceso, String nombreArchivo, int cantidadCobros,
        BigDecimal montoTotal) {
}
