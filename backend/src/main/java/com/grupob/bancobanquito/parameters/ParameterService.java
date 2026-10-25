package com.grupob.bancobanquito.parameters;

import java.math.BigDecimal;

public interface ParameterService {

    BigDecimal getValue(String parameterCode);
}
