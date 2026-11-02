package com.grupob.bancobanquito.amortizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ServiceAmortizacion {

    List<CuotaCalculada> calculate(BigDecimal monto, BigDecimal teaAsignada, Integer plazoMeses,
                                          MetodoAmortizacion metodo, LocalDate fechaDesembolso);

    BigDecimal monthlyRate(BigDecimal teaAsignada);

    BigDecimal interesPorDias(BigDecimal saldoCapital, BigDecimal teaAsignada, long dias, Integer baseDias);

    List<CuotaCalculada> recalcular(RecalculoTabla recalculo);
}
