package com.grupob.bancobanquito.parameters;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ParameterService {

    BigDecimal getValue(String parameterCode);

    BigDecimal getValue(String parameterCode, LocalDate date);
}
