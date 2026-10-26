package com.grupob.bancobanquito.parametros.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.parametros.ParametroNoEncontradoException;
import com.grupob.bancobanquito.parametros.ServiceParametro;
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
    ServiceParametro servicioParametro;

    @Autowired
    ServiceFechaNegocio servicioFechaNegocio;

    @Autowired
    ParametroRepository parameterRepository;

    @Test
    void currentBusinessDateIsTheOpenDay() {
        assertThat(servicioFechaNegocio.getCurrentBusinessDate()).isEqualTo(FIRST_DAY);
    }

    @Test
    void returnsTheValueInForceOnTheBusinessDate() {
        assertThat(servicioParametro.getValue("PAR-07")).isEqualByComparingTo("0.0006");
        assertThat(servicioParametro.getValue("PAR-08")).isEqualByComparingTo("0.0050");
    }

    @Test
    void keepsTheHistoryWhenAParameterChanges() {
        LocalDate changeDate = FIRST_DAY.plusDays(10);
        Parametro current = parameterRepository
                .findFirstByCodeAndValidFromLessThanEqualOrderByValidFromDesc("PAR-06", FIRST_DAY).orElseThrow();
        current.setValidTo(changeDate);
        current.setModifiedBy("pruebas");
        current.setModifiedAt(LocalDateTime.now());

        Parametro next = new Parametro();
        next.setCode("PAR-06");
        next.setName(current.getName());
        next.setValue(new BigDecimal("0.1600"));
        next.setValidFrom(changeDate);
        next.setCreatedBy("pruebas");
        next.setCreatedAt(LocalDateTime.now());
        parameterRepository.saveAndFlush(next);

        assertThat(servicioParametro.getValue("PAR-06", changeDate.minusDays(1))).isEqualByComparingTo("0.1550");
        assertThat(servicioParametro.getValue("PAR-06", changeDate)).isEqualByComparingTo("0.1600");
    }

    @Test
    void failsWhenTheParameterDoesNotExist() {
        assertThatThrownBy(() -> servicioParametro.getValue("PAR-99"))
                .isInstanceOf(ParametroNoEncontradoException.class)
                .hasMessageContaining("PAR-99");
    }

    @Test
    void failsBeforeTheParameterStarts() {
        assertThatThrownBy(() -> servicioParametro.getValue("PAR-07", FIRST_DAY.minusDays(1)))
                .isInstanceOf(ParametroNoEncontradoException.class);
    }
}
