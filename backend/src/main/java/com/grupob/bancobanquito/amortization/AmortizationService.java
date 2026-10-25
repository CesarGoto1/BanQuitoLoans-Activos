package com.grupob.bancobanquito.amortization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AmortizationService {

    List<CalculatedInstallment> calculate(BigDecimal amount, BigDecimal effectiveAnnualRate, Integer termMonths,
                                          AmortizationMethod method, LocalDate disbursementDate);

    BigDecimal monthlyRate(BigDecimal effectiveAnnualRate);
}
