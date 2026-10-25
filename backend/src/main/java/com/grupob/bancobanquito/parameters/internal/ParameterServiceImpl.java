package com.grupob.bancobanquito.parameters.internal;

import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterNotFoundException;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ParameterServiceImpl implements ParameterService {

    private final ParameterRepository parameterRepository;
    private final BusinessDateService businessDateService;

    ParameterServiceImpl(ParameterRepository parameterRepository, BusinessDateService businessDateService) {
        this.parameterRepository = parameterRepository;
        this.businessDateService = businessDateService;
    }

    @Override
    public BigDecimal getValue(String parameterCode) {
        return getValue(parameterCode, businessDateService.getCurrentBusinessDate());
    }

    @Override
    public BigDecimal getValue(String parameterCode, LocalDate date) {
        return parameterRepository.findFirstByCodeAndValidFromLessThanEqualOrderByValidFromDesc(parameterCode, date)
                .filter(parameter -> parameter.isValidOn(date))
                .map(Parameter::getValue)
                .orElseThrow(() -> new ParameterNotFoundException(
                        "El parámetro " + parameterCode + " no está vigente el " + date + "."));
    }
}
