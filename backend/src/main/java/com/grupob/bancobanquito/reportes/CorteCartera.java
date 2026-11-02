package com.grupob.bancobanquito.reportes;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CorteCartera(LocalDate fechaCorte, List<Operacion> operaciones, List<Tramo> tramos) {

    public CorteCartera {
        if (fechaCorte == null || operaciones == null || tramos == null) {
            throw new IllegalArgumentException("La fecha, las operaciones y los tramos del corte son obligatorios.");
        }
        operaciones = List.copyOf(operaciones);
        tramos = List.copyOf(tramos);
    }

    public record Operacion(Integer idOperacion, Integer idTramo, EstadoCartera estadoCartera,
                            BigDecimal saldoCapital, BigDecimal provisionConstituida) {
    }

    public record Tramo(Integer idTramo, Integer numeroTramo, Integer diasMoraDesde, Integer diasMoraHasta,
                        EstadoCartera estadoCartera, BigDecimal porcentajeProvision) {
    }
}
