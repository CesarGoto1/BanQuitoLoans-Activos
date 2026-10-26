package com.grupob.bancobanquito.parametros.internal;

import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ParametroNoEncontradoException;
import com.grupob.bancobanquito.parametros.ServiceParametro;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ServiceParametroImpl implements ServiceParametro {

    private final ParametroRepository parameterRepository;
    private final ServiceFechaNegocio servicioFechaNegocio;

    ServiceParametroImpl(ParametroRepository parameterRepository, ServiceFechaNegocio servicioFechaNegocio) {
        this.parameterRepository = parameterRepository;
        this.servicioFechaNegocio = servicioFechaNegocio;
    }

    @Override
    public BigDecimal getValue(String parameterCode) {
        return getValue(parameterCode, servicioFechaNegocio.getCurrentBusinessDate());
    }

    @Override
    public BigDecimal getValue(String parameterCode, LocalDate date) {
        return parameterRepository.findFirstByCodeAndValidFromLessThanEqualOrderByValidFromDesc(parameterCode, date)
                .filter(parameter -> parameter.isValidOn(date))
                .map(Parametro::getValue)
                .orElseThrow(() -> new ParametroNoEncontradoException(
                        "El parámetro " + parameterCode + " no está vigente el " + date + "."));
    }
}
