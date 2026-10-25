package com.grupob.bancobanquito.parameters.internal;

import com.grupob.bancobanquito.parameters.BusinessDateService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
class SimulatedBusinessDateService implements BusinessDateService {

    @Override
    public LocalDate getCurrentBusinessDate() {
        return LocalDate.of(2026, 10, 31);
    }
}
