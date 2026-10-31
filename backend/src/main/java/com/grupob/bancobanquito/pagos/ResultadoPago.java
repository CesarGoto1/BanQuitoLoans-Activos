package com.grupob.bancobanquito.pagos;

import java.math.BigDecimal;

public record ResultadoPago(
        EstadoAplicacionPago estado,
        String codigoRechazo,
        BigDecimal aplicadoSeguro,
        BigDecimal aplicadoMora,
        BigDecimal aplicadoInteres,
        BigDecimal aplicadoCapital,
        BigDecimal aplicadoAnticipado,
        BigDecimal excedenteDevolver) {
}
