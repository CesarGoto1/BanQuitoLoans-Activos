package com.grupob.bancobanquito.acceso.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simulated")
@Transactional
class AnalystServiceTest {

    @Autowired
    ServiceAnalista analystService;

    @Autowired
    AnalistaRepository analystRepository;

    @Test
    void logsInAnExistingAnalyst() {
        Analista analyst = new Analista();
        analyst.setCedula("1799999991");
        analyst.setNombre("Analista de prueba");
        analyst.setCreatedBy("pruebas");
        analyst.setCreatedAt(LocalDateTime.now());
        analystRepository.save(analyst);

        SesionAnalista session = analystService.login("1799999991");

        assertThat(session.idAnalista()).isEqualTo(analyst.getId());
        assertThat(session.nombre()).isEqualTo("Analista de prueba");
        assertThat(analystService.findAnalyst(analyst.getId())).contains(session);
    }

    @Test
    void rejectsAnUnknownNationalId() {
        assertThatThrownBy(() -> analystService.login("1700000000"))
                .isInstanceOfSatisfying(ExcepcionAcceso.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
