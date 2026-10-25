package com.grupob.bancobanquito.parametrizacion.internal;

import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parametrizacion.FechaNegocioService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FechaNegocioServiceImpl implements FechaNegocioService {

    private final BusinessDateService businessDateService;

    public FechaNegocioServiceImpl(BusinessDateService businessDateService) {
        this.businessDateService = businessDateService;
    }

    @Override
    public LocalDate fechaActual() {
        return this.businessDateService.getCurrentBusinessDate();
    }
}
