package com.grupob.bancobanquito.pagos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoRecaudacion(
        Integer idRegistro,
        UUID uuidTransaccion,
        String numeroOperacion,
        String cedulaCliente,
        BigDecimal monto,
        LocalDateTime fechaHoraCobro,
        String canal,
        LocalDate fechaNegocio) {
}
