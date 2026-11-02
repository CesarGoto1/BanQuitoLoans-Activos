package com.grupob.bancobanquito.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResultadoConcentracion(LocalDate fechaCorte, BigDecimal carteraBruta, long cantidadClientes,
                                     List<Cliente> mayoresClientes, BigDecimal saldoMayoresClientes,
                                     BigDecimal concentracionDiezMayores) {

    public ResultadoConcentracion {
        mayoresClientes = List.copyOf(mayoresClientes);
    }

    public record Cliente(Integer idCliente, long cantidadOperaciones, BigDecimal saldoCapital,
                          BigDecimal participacionCarteraBruta) {
    }
}
