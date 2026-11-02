package com.grupob.bancobanquito.cobranza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenFotoCartera(
        Integer idOperacion,
        LocalDate fechaFoto,
        Integer idTramo,
        String estadoCartera,
        int diasMora,
        BigDecimal saldoCapital,
        BigDecimal valorProvision,
        BigDecimal interesDevengadoAcumulado,
        BigDecimal interesMoraAcumulado
) {
}
