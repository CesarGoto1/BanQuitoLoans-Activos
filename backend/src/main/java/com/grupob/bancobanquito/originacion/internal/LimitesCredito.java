package com.grupob.bancobanquito.originacion.internal;

import java.math.BigDecimal;

record LimitesCredito(
        BigDecimal montoMinimo,
        BigDecimal montoMaximo,
        Integer plazoMinimoMeses,
        Integer plazoMaximoMeses) {
}
