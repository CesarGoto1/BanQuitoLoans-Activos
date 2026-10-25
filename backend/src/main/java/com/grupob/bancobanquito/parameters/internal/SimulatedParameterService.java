package com.grupob.bancobanquito.parameters.internal;

import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class SimulatedParameterService implements ParameterService {

    private static final Map<String, BigDecimal> VALUES = Map.of(
            "PAR-07", new BigDecimal("0.0006"),
            "PAR-08", new BigDecimal("0.0050"));

    @Override
    public BigDecimal getValue(String parameterCode) {
        BigDecimal value = VALUES.get(parameterCode);
        if (value == null) {
            throw new IllegalArgumentException("Parameter " + parameterCode + " does not exist.");
        }
        return value;
    }
}
