package com.grupob.bancobanquito.prestamos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoDesembolso(
        Integer idSolicitud,
        String numeroOperacion,
        BigDecimal montoNeto,
        String cuentaDestino,
        LocalDate fechaDesembolso) {
}
