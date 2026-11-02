package com.grupob.bancobanquito.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResultadoComposicionCartera(LocalDate fechaCorte, List<DetalleTramo> tramos,
                                         long cantidadOperaciones, BigDecimal saldoCapital,
                                         BigDecimal provisionConstituida) {

    public ResultadoComposicionCartera {
        tramos = List.copyOf(tramos);
    }

    public record DetalleTramo(CorteCartera.Tramo tramo, long cantidadOperaciones, BigDecimal saldoCapital,
                               BigDecimal participacion, BigDecimal provisionConstituida) {
    }
}
