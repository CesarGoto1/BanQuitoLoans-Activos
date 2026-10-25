package com.grupob.bancobanquito.parametrizacion.internal;

import com.grupob.bancobanquito.parameters.ParameterService;
import com.grupob.bancobanquito.parametrizacion.ParametroService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ParametroServiceImpl implements ParametroService {

    private final ParameterService parameterService;

    public ParametroServiceImpl(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @Override
    public BigDecimal valor(String codigoParametro) {
        return this.parameterService.getValue(codigoParametro);
    }
}
