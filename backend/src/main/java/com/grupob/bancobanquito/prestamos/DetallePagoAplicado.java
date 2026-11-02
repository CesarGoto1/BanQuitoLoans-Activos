package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;

public record DetallePagoAplicado(
        Integer numeroCuota,
        BigDecimal aplicadoSeguro,
        BigDecimal aplicadoMora,
        BigDecimal aplicadoInteres,
        BigDecimal aplicadoCapital) {
}
