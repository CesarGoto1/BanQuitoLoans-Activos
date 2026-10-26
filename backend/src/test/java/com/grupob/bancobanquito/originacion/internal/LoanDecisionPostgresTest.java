package com.grupob.bancobanquito.originacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;

import com.grupob.bancobanquito.originacion.ServiceDecisionPrestamo;
import com.grupob.bancobanquito.originacion.ServiceBandejaSolicitud;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RF05_POSTGRES_TESTS", matches = "true")
@Sql(scripts = "/rf05-analysts.sql", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class LoanDecisionPostgresTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ServiceDecisionPrestamo decisions;
    @Autowired
    private ServiceBandejaSolicitud review;
    @Autowired
    private SolicitudRepository applications;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private EntityManager entityManager;
    private final List<Integer> createdApplications = new ArrayList<>();

    @Test
    void resolvesThroughRealHttpAndReturnsConflictOnReplay() throws Exception {
        Integer id = registerApplication("FRANCES", "3500.00");
        try (HttpClient client = HttpClient.newHttpClient()) {
            var approval = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                            + "/api/analista/solicitudes/" + id + "/aprobacion"))
                    .header("Content-Type", "application/json")
                    .header("X-Rol", "ANALISTA").header("X-Identificacion", "1799999901")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"teaAsignada\":0.1677}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(approval.statusCode()).isEqualTo(200);
            assertThat(approval.body()).contains("\"estadoSolicitud\":\"APROBADA\"", "\"cuotaEvaluacionDefinitiva\":911.44");
            var replay = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                            + "/api/analista/solicitudes/" + id + "/rechazo"))
                    .header("Content-Type", "application/json")
                    .header("X-Rol", "ANALISTA").header("X-Identificacion", "1799999902")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"motivoRechazo\":\"Segundo intento\"}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(replay.statusCode()).isEqualTo(409);
            assertThat(replay.headers().firstValue("Content-Type")).hasValue("application/problem+json");
            assertThat(applications.findById(id).orElseThrow().getStatus()).isEqualTo(EstadoSolicitud.APROBADA);
        }
    }

    private Integer registerApplication(String system, String income) {
        Solicitud application = new Solicitud();
        application.setCustomerId(101);
        application.setAmortizationMethod(MetodoAmortizacion.valueOf(system));
        application.setRequestedAmount(new BigDecimal("10000.00"));
        application.setTermMonths(12);
        application.setPurpose(DestinoCredito.EDUCACION);
        application.setMonthlyIncome(new BigDecimal(income));
        application.setMonthlyExpenses(new BigDecimal("1200.00"));
        application.setApplicationDate(LocalDate.of(2026, 10, 31));
        application.setRegisteredAt(LocalDateTime.now());
        application.setReferenceInstallment(new BigDecimal("900.00"));
        application.setStatus(EstadoSolicitud.REGISTRADA);
        application.setCreatedBy("rf05-test-customer");
        application.setCreatedAt(LocalDateTime.now());
        Integer id = applications.saveAndFlush(application).getId();
        createdApplications.add(id);
        return id;
    }

    @AfterEach
    void cleanUpApplications() {
        createdApplications.forEach(applications::deleteById);
    }

    @Test
    void persistsFrenchApprovalAndKeepsReviewAndInboxConsistent() {
        Integer id = registerApplication("FRANCES", "3500.00");
        assertThat(review.listarBandejaAnalista("REGISTRADA")).anyMatch(item -> item.idSolicitud().equals(id));
        decisions.approveApplication(id, "1799999901", new BigDecimal("0.1677"));
        Solicitud stored = applications.findById(id).orElseThrow();
        assertThat(stored.getAssignedRate()).isEqualByComparingTo("0.1677");
        assertThat(stored.getFinalInstallment()).isEqualByComparingTo("911.44");
        assertThat(stored.getAnalystId()).isNotNull();
        assertThat(stored.getResolutionDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(stored.getLastModifiedBy()).isEqualTo("1799999901");
        assertThat(stored.getLastModifiedAt()).isAfterOrEqualTo(stored.getCreatedAt());
        assertThat(stored.getCreatedBy()).isEqualTo("rf05-test-customer");
        assertThat(stored.getRejectionReason()).isNull();
        assertThat(review.obtenerDetalleParaEvaluacion(id).estadoSolicitud()).isEqualTo("APROBADA");
        assertThat(review.listarBandejaAnalista("REGISTRADA")).noneMatch(item -> item.idSolicitud().equals(id));
        assertThat(operationCount(id)).isZero();
    }

    @Test
    void refusesGermanReferenceExampleAndRollsBackAllChanges() {
        Integer id = registerApplication("ALEMAN", "3500.00");
        assertThatThrownBy(() -> decisions.approveApplication(id, "1799999901", new BigDecimal("0.1677")))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
        Solicitud stored = applications.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(EstadoSolicitud.REGISTRADA);
        assertThat(stored.getAssignedRate()).isNull();
        assertThat(stored.getFinalInstallment()).isNull();
        assertThat(stored.getLastModifiedBy()).isNull();
        assertThat(operationCount(id)).isZero();
    }

    @Test
    void persistsHighestGermanInstallmentIncludingInsurance() {
        Integer id = registerApplication("ALEMAN", "5000.00");
        decisions.approveApplication(id, "1799999901", new BigDecimal("0.1677"));
        assertThat(applications.findById(id).orElseThrow().getFinalInstallment())
                .isEqualByComparingTo("969.36");
    }

    @Test
    void persistsRejectionAndPreventsBothLaterDecisions() {
        Integer id = registerApplication("FRANCES", "3500.00");
        decisions.rejectApplication(id, "1799999901", "Documentación insuficiente.");
        Solicitud stored = applications.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(stored.getRejectionReason()).isEqualTo("Documentación insuficiente.");
        assertThat(stored.getAssignedRate()).isNull();
        assertThat(stored.getFinalInstallment()).isNull();
        assertThat(stored.getAnalystId()).isNotNull();
        assertThat(stored.getResolutionDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThatThrownBy(() -> decisions.rejectApplication(id, "1799999902", "Otro motivo"))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
        assertThatThrownBy(() -> decisions.approveApplication(id, "1799999902", new BigDecimal("0.16")))
                .isInstanceOf(ExcepcionReglaDecisionPrestamo.class);
        assertThat(operationCount(id)).isZero();
    }

    @Test
    void rollsBackAFlushedDecisionWhenTheTransactionFails() {
        Integer id = registerApplication("FRANCES", "3500.00");
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            decisions.rejectApplication(id, "1799999901", "Motivo de prueba");
            throw new IllegalStateException("Induced failure after flush");
        })).isInstanceOf(IllegalStateException.class);
        Solicitud stored = applications.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(EstadoSolicitud.REGISTRADA);
        assertThat(stored.getRejectionReason()).isNull();
        assertThat(stored.getAnalystId()).isNull();
        assertThat(stored.getResolutionDate()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void permitsExactlyOneConcurrentDecision(int attempt) throws Exception {
        Integer id = registerApplication("FRANCES", "3500.00");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var approval = executor.submit(() -> decideConcurrently(id, true, ready, start));
            var rejection = executor.submit(() -> decideConcurrently(id, false, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(approval.get(10, TimeUnit.SECONDS), rejection.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "CONFLICT");
        }
        Solicitud stored = applications.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isIn(EstadoSolicitud.APROBADA, EstadoSolicitud.RECHAZADA);
        assertThat(operationCount(id)).isZero();
    }

    private String decideConcurrently(Integer id, boolean approve, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test did not start");
        try {
            if (approve) decisions.approveApplication(id, "1799999901", new BigDecimal("0.1677"));
            else decisions.rejectApplication(id, "1799999902", "Rechazo concurrente");
            return "SUCCESS";
        } catch (ExcepcionReglaDecisionPrestamo | PessimisticLockingFailureException exception) {
            return "CONFLICT";
        }
    }

    private long operationCount(Integer id) {
        return new TransactionTemplate(transactionManager).execute(status -> ((Number) entityManager
                .createNativeQuery("select count(*) from operaciones where \"idSolicitud\" = :id")
                .setParameter("id", id).getSingleResult()).longValue());
    }
}
