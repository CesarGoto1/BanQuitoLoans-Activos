package com.grupob.bancobanquito.parametros;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ServiceParametro {

    BigDecimal getValue(String parameterCode);

    BigDecimal getValue(String parameterCode, LocalDate date);
}
