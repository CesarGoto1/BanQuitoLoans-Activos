package com.grupob.bancobanquito.pasivos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstruccionDesembolso(
        String numeroOperacion,
        String cedulaCliente,
        String cuentaDestino,
        BigDecimal montoAprobado,
        BigDecimal contribucionSolca,
        BigDecimal montoNeto,
        LocalDate fechaDesembolso) {
}
