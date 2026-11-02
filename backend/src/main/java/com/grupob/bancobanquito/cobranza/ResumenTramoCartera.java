package com.grupob.bancobanquito.cobranza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenTramoCartera(
        Integer idTramo,
        Integer numeroTramo,
        Integer diasMoraDesde,
        Integer diasMoraHasta,
        String estadoCartera,
        Boolean devengaIngresos,
        String tratamientoMora,
        BigDecimal porcentajeProvision,
        LocalDate fechaVigenciaDesde,
        LocalDate fechaVigenciaHasta
) {
}
