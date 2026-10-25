package com.grupob.bancobanquito.access.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.access.AnalystService;
import com.grupob.bancobanquito.access.AnalystSession;
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
    AnalystService analystService;

    @Autowired
    AnalystRepository analystRepository;

    @Test
    void logsInAnExistingAnalyst() {
        Analyst analyst = new Analyst();
        analyst.setNationalId("1799999991");
        analyst.setName("Analista de prueba");
        analyst.setCreatedBy("pruebas");
        analyst.setCreatedAt(LocalDateTime.now());
        analystRepository.save(analyst);

        AnalystSession session = analystService.login("1799999991");

        assertThat(session.analystId()).isEqualTo(analyst.getId());
        assertThat(session.name()).isEqualTo("Analista de prueba");
        assertThat(analystService.findAnalyst(analyst.getId())).contains(session);
    }

    @Test
    void rejectsAnUnknownNationalId() {
        assertThatThrownBy(() -> analystService.login("1700000000"))
                .isInstanceOfSatisfying(AccessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
