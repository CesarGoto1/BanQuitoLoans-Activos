package com.grupob.bancobanquito.parametros.internal;

import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ServiceFechaNegocioImpl implements ServiceFechaNegocio {

    private final DiaNegocioRepository businessDayRepository;

    ServiceFechaNegocioImpl(DiaNegocioRepository businessDayRepository) {
        this.businessDayRepository = businessDayRepository;
    }

    @Override
    public LocalDate getCurrentBusinessDate() {
        return businessDayRepository.findFirstByStatusNotOrderByFechaNegocioAsc(EstadoCierre.COMPLETED)
                .map(DiaNegocio::getFechaNegocio)
                .orElseThrow(() -> new IllegalStateException("No hay una fecha de negocio abierta."));
    }
}
