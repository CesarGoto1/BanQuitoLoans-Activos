package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;
import java.util.List;

public record OperacionPorPagar(
        Integer idOperacion,
        String numeroOperacion,
        String cedulaCliente,
        EstadoCartera estadoCartera,
        BigDecimal saldoCapital,
        List<CuotaPorPagar> cuotasPendientes) {
}
