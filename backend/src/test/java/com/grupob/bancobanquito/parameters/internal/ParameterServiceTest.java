package com.grupob.bancobanquito.parameters.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterNotFoundException;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ParameterServiceTest {

    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 10, 31);

    @Autowired
    ParameterService parameterService;

    @Autowired
    BusinessDateService businessDateService;

    @Autowired
    ParameterRepository parameterRepository;

    @Test
    void currentBusinessDateIsTheOpenDay() {
        assertThat(businessDateService.getCurrentBusinessDate()).isEqualTo(FIRST_DAY);
    }

    @Test
    void returnsTheValueInForceOnTheBusinessDate() {
        assertThat(parameterService.getValue("PAR-07")).isEqualByComparingTo("0.0006");
        assertThat(parameterService.getValue("PAR-08")).isEqualByComparingTo("0.0050");
    }

    @Test
    void keepsTheHistoryWhenAParameterChanges() {
        LocalDate changeDate = FIRST_DAY.plusDays(10);
        Parameter current = parameterRepository
                .findFirstByCodeAndValidFromLessThanEqualOrderByValidFromDesc("PAR-06", FIRST_DAY).orElseThrow();
        current.setValidTo(changeDate);
        current.setModifiedBy("pruebas");
        current.setModifiedAt(LocalDateTime.now());

        Parameter next = new Parameter();
        next.setCode("PAR-06");
        next.setName(current.getName());
        next.setValue(new BigDecimal("0.1600"));
        next.setValidFrom(changeDate);
        next.setCreatedBy("pruebas");
        next.setCreatedAt(LocalDateTime.now());
        parameterRepository.saveAndFlush(next);

        assertThat(parameterService.getValue("PAR-06", changeDate.minusDays(1))).isEqualByComparingTo("0.1550");
        assertThat(parameterService.getValue("PAR-06", changeDate)).isEqualByComparingTo("0.1600");
    }

    @Test
    void failsWhenTheParameterDoesNotExist() {
        assertThatThrownBy(() -> parameterService.getValue("PAR-99"))
                .isInstanceOf(ParameterNotFoundException.class)
                .hasMessageContaining("PAR-99");
    }

    @Test
    void failsBeforeTheParameterStarts() {
        assertThatThrownBy(() -> parameterService.getValue("PAR-07", FIRST_DAY.minusDays(1)))
                .isInstanceOf(ParameterNotFoundException.class);
    }
}
