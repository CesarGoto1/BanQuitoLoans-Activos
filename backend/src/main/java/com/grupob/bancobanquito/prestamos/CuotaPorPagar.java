package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaPorPagar(
        Integer idCuota,
        Integer numeroCuota,
        LocalDate fechaVencimiento,
        BigDecimal seguroPendiente,
        BigDecimal moraPendiente,
        BigDecimal interesPendiente,
        BigDecimal capitalPendiente) {
}
