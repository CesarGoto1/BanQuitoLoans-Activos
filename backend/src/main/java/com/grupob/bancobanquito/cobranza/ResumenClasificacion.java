package com.grupob.bancobanquito.cobranza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenClasificacion(
        Integer idOperacion,
        String numeroOperacion,
        int diasMora,
        String estadoAnterior,
        String estadoNuevo,
        BigDecimal saldoCapital,
        boolean cambioEstado,
        LocalDate fechaNegocio
) {
}
