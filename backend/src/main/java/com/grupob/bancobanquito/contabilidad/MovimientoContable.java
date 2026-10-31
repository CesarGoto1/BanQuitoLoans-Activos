package com.grupob.bancobanquito.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoContable(
        Integer idOperacion,
        LocalDate fechaAsiento,
        TipoAsiento tipoAsiento,
        CodigoCuentaContable cuentaDebito,
        CodigoCuentaContable cuentaCredito,
        BigDecimal monto,
        String claveIdempotencia) {
}
