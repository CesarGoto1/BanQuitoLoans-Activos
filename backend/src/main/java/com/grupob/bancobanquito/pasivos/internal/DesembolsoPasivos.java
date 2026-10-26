package com.grupob.bancobanquito.pasivos.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

record DesembolsoPasivos(
        String version,
        String numeroPrestamo,
        String cedulaCliente,
        String cuentaDestino,
        BigDecimal montoAprobado,
        BigDecimal contribucionSolca,
        BigDecimal montoNeto,
        LocalDate fechaDesembolso,
        String referencia) {
}
