package com.grupob.bancobanquito.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResultadoRollRate(LocalDate fechaInicial, LocalDate fechaFinal, List<Fila> filas,
                                long cantidadOperacionesIniciales, BigDecimal saldoInicial) {

    public ResultadoRollRate {
        filas = List.copyOf(filas);
    }

    public record Fila(CorteCartera.Tramo tramoInicial, long cantidadOperaciones, BigDecimal saldoInicial,
                       List<Transicion> transiciones) {
        public Fila {
            transiciones = List.copyOf(transiciones);
        }
    }

    public record Transicion(Integer idTramoFinal, String destino, long cantidadOperaciones,
                             BigDecimal saldoInicial, BigDecimal porcentaje) {
    }
}
