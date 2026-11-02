package com.grupob.bancobanquito.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoMorosidad(LocalDate fechaCorte, BigDecimal saldoPorVencer, BigDecimal saldoVencido,
                                 BigDecimal saldoEnLitigio, BigDecimal carteraBruta, BigDecimal saldoCastigado,
                                 BigDecimal indiceMorosidad) {
}
