package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PagoAplicado(
        Integer idPago,
        LocalDate fechaPago,
        String tipoPago,
        String modalidadAbono,
        BigDecimal montoRecibido,
        BigDecimal aplicadoSeguro,
        BigDecimal aplicadoMora,
        BigDecimal aplicadoInteres,
        BigDecimal aplicadoCapital,
        String registradoPor,
        LocalDateTime fechaRegistro,
        List<DetallePagoAplicado> detalles) {
}
