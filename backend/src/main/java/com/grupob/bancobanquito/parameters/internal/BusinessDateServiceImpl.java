package com.grupob.bancobanquito.parameters.internal;

import com.grupob.bancobanquito.parameters.BusinessDateService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class BusinessDateServiceImpl implements BusinessDateService {

    private final BusinessDayRepository businessDayRepository;

    BusinessDateServiceImpl(BusinessDayRepository businessDayRepository) {
        this.businessDayRepository = businessDayRepository;
    }

    @Override
    public LocalDate getCurrentBusinessDate() {
        return businessDayRepository.findFirstByStatusNotOrderByBusinessDateAsc(ClosingStatus.COMPLETED)
                .map(BusinessDay::getBusinessDate)
                .orElseThrow(() -> new IllegalStateException("No hay una fecha de negocio abierta."));
    }
}
