package com.grupob.bancobanquito.origination.internal;

import java.math.BigDecimal;

record CreditLimits(
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer minTermMonths,
        Integer maxTermMonths) {
}
