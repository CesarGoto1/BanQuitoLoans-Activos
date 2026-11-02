package com.grupob.bancobanquito.contabilidad;

import java.math.BigDecimal;

public record DatosProvision(
        Integer idOperacion,
        BigDecimal saldoCapital,
        BigDecimal porcentajeProvision,
        BigDecimal provisionAnterior,
        String estadoCartera) {
}
