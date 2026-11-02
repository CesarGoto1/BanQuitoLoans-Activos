package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.prestamos.VistaPreviaDesembolso;
import com.grupob.bancobanquito.prestamos.ResultadoDesembolso;
import com.grupob.bancobanquito.prestamos.ServicePrestamo;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import com.grupob.bancobanquito.originacion.internal.SolicitudesDePrueba;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import com.grupob.bancobanquito.pasivos.*;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import java.time.format.DateTimeFormatter;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("simulated")
@Transactional
class LoanServiceTest {

    @Autowired
    ServicePrestamo loanService;

    @Autowired
    PrestamoRepository loanRepository;

    @Autowired
    CuotaRepository installmentRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;
    @MockitoBean
    ServicePasivos pasivos;
    @MockitoBean
    ServiceFechaNegocio fechaNegocio;
    private Integer cliente;

    @BeforeEach
    void prepararCliente() {
        cliente = SolicitudesDePrueba.clienteIndependiente(entityManager);
        when(fechaNegocio.getCurrentBusinessDate()).thenReturn(SolicitudesDePrueba.FECHA);
        when(pasivos.findCustomer(cliente)).thenReturn(Optional.of(new Cliente(cliente, "1710034065", "Cliente", "Prueba")));
        when(pasivos.findActiveAccounts(cliente)).thenReturn(List.of(new Cuenta("2200000001", "AHORROS")));
    }

    @Test
    void previewCalculatesInsuranceSolcaAndNetAmount() {
        VistaPreviaDesembolso preview = loanService.previewDisbursement(SolicitudesDePrueba.crearAprobada(entityManager, cliente, "10000.00", 12, MetodoAmortizacion.FRANCES));

        assertThat(preview.seguroMensual()).isEqualByComparingTo("6.00");
        assertThat(preview.contribucionSolca()).isEqualByComparingTo("50.00");
        assertThat(preview.montoNeto()).isEqualByComparingTo("9950.00");
        assertThat(preview.cuentas()).isNotEmpty();
    }

    @Test
    void disbursesAndSavesTheLoanWithItsSchedule() {
        Integer idSolicitud = SolicitudesDePrueba.crearAprobada(entityManager, cliente, "5000.00", 12, MetodoAmortizacion.ALEMAN);
        ResultadoDesembolso result = loanService.disburse(idSolicitud, "2200000001");

        assertThat(result.numeroOperacion()).isEqualTo("OP-" + result.fechaDesembolso().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", idSolicitud));
        assertThat(result.montoNeto()).isEqualByComparingTo("4975.00");

        Prestamo loan = loanRepository.findByNumeroOperacion(result.numeroOperacion()).orElseThrow();
        assertThat(loan.getPortfolioStatus()).isEqualTo(EstadoCartera.POR_VENCER);
        assertThat(loan.getDisbursementInstructionStatus()).isEqualTo(EstadoInstruccionDesembolso.ENTREGADA);
        assertThat(installmentRepository.countByLoanId(loan.getId())).isEqualTo(12);

        BigDecimal totalPrincipal = installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(loan.getId()).stream()
                .map(Cuota::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipal).isEqualByComparingTo(loan.getApprovedAmount());
    }

    @Test
    void doesNotDisburseTwice() {
        Integer idSolicitud = SolicitudesDePrueba.crearAprobada(entityManager, cliente, "10000.00", 12, MetodoAmortizacion.FRANCES);
        loanService.disburse(idSolicitud, "2200000001");

        assertThatThrownBy(() -> loanService.disburse(idSolicitud, "2200000001"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(loanService.findDisbursement(idSolicitud)).isPresent();
    }

    @Test
    void doesNotDisburseAnApplicationThatIsNotApproved() {
        Integer idSolicitud = SolicitudesDePrueba.crearAprobada(entityManager, cliente, "10000.00", 12, MetodoAmortizacion.FRANCES);
        SolicitudesDePrueba.marcarRegistrada(entityManager, idSolicitud);
        assertThatThrownBy(() -> loanService.disburse(idSolicitud, "2200000001"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void doesNotDisburseToAnAccountOfAnotherCustomer() {
        Integer idSolicitud = SolicitudesDePrueba.crearAprobada(entityManager, cliente, "10000.00", 12, MetodoAmortizacion.FRANCES);
        assertThatThrownBy(() -> loanService.disburse(idSolicitud, "2200000002"))
                .isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void leavesNothingSavedWhenTheLiabilitiesCoreFails() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        Integer solicitud = transaccion.execute(estado -> SolicitudesDePrueba.crearAprobada(
                entityManager, cliente, "10000.00", 12, MetodoAmortizacion.FRANCES));
        java.util.concurrent.atomic.AtomicReference<Integer> operacion = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            doAnswer(invocacion -> {
                Prestamo prestamo = loanRepository.findByIdSolicitud(solicitud).orElseThrow();
                operacion.set(prestamo.getId());
                assertThat(installmentRepository.countByLoanId(prestamo.getId())).isEqualTo(12);
                assertThat(entityManager.createQuery("select count(a) from AsientoContable a where a.idOperacion = :id", Long.class)
                        .setParameter("id", prestamo.getId()).getSingleResult()).isEqualTo(2);
                throw new IllegalStateException("Pasivos no disponible");
            }).when(pasivos).creditDisbursement(any());
            assertThat(org.springframework.aop.support.AopUtils.isAopProxy(loanService)).isTrue();
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThatThrownBy(() -> loanService.disburse(solicitud, "2200000001"))
                    .isInstanceOf(ExcepcionPrestamo.class)
                    .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(operacion.get()).isNotNull();
            transaccion.executeWithoutResult(estado -> {
                assertThat(loanRepository.existsByIdSolicitud(solicitud)).isFalse();
                assertThat(installmentRepository.countByLoanId(operacion.get())).isZero();
                assertThat(entityManager.createQuery("select count(a) from AsientoContable a where a.idOperacion = :id", Long.class)
                        .setParameter("id", operacion.get()).getSingleResult()).isZero();
            });
        } finally {
            transaccion.executeWithoutResult(estado -> {
                loanRepository.findByIdSolicitud(solicitud).ifPresent(prestamo -> {
                    entityManager.createQuery("select a from AsientoContable a where a.idOperacion = :id")
                            .setParameter("id", prestamo.getId()).getResultList().forEach(entityManager::remove);
                    installmentRepository.findByLoanIdOrderByNumeroCuotaAsc(prestamo.getId()).forEach(entityManager::remove);
                    entityManager.remove(prestamo);
                    entityManager.flush();
                });
                SolicitudesDePrueba.eliminarPreparacion(entityManager, solicitud);
            });
        }
    }
}
