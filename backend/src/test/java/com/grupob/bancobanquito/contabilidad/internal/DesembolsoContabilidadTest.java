package com.grupob.bancobanquito.contabilidad.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.contabilidad.AsientoInvalidoException;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import com.grupob.bancobanquito.pasivos.Cliente;
import com.grupob.bancobanquito.pasivos.Cuenta;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import com.grupob.bancobanquito.prestamos.ServicePrestamo;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.grupob.bancobanquito.originacion.internal.SolicitudesDePrueba;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Transactional
class DesembolsoContabilidadTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);

    @Autowired
    ServicePrestamo servicioPrestamo;
    @MockitoSpyBean
    ServiceContabilidad servicioContabilidad;
    @Autowired
    AsientoContableRepository asientoRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    PlatformTransactionManager transactionManager;
    @MockitoBean
    ServicePasivos servicioPasivos;
    @MockitoBean
    ServiceFechaNegocio servicioFechaNegocio;
    @Captor
    ArgumentCaptor<List<MovimientoContable>> movimientosRegistrados;

    private Integer idSolicitud;
    private Integer idOperacion;

    @BeforeEach
    void prepararDesembolso() {
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(FECHA);
        Integer cliente = SolicitudesDePrueba.clienteIndependiente(entityManager);
        when(servicioPasivos.findCustomer(cliente)).thenReturn(Optional.of(
                new Cliente(cliente, "1710034065", "Pepito", "Perez")));
        when(servicioPasivos.findActiveAccounts(cliente)).thenReturn(List.of(new Cuenta("2200000001", "AHORROS")));
        idSolicitud = SolicitudesDePrueba.crearAprobada(entityManager, cliente, "10000.00", 2,
                com.grupob.bancobanquito.amortizacion.MetodoAmortizacion.FRANCES);
        assertThat(servicioPrestamo.findDisbursement(idSolicitud)).isEmpty();
    }

    @Test
    void persisteLosDosAsientosYNoLosDuplicaAlRepetirElLote() {
        servicioPrestamo.disburse(idSolicitud, "2200000001");
        entityManager.flush();

        List<AsientoContable> asientos = asientosDesembolso();
        assertThat(asientos).hasSize(2);
        assertThat(asientos).allSatisfy(asiento -> {
            assertThat(asiento.getFechaAsiento()).isEqualTo(FECHA);
            assertThat(asiento.getTipoAsiento()).isEqualTo(TipoAsiento.DESEMBOLSO);
            assertThat(asiento.getCuentaDebito().getCodigoCuenta())
                    .isEqualTo(CodigoCuentaContable.CARTERA_POR_VENCER);
        });
        assertThat(asientos.get(0).getCuentaCredito().getCodigoCuenta()).isEqualTo(CodigoCuentaContable.CUENTA_CLIENTE);
        assertThat(asientos.get(0).getMontoAsiento()).isEqualByComparingTo("9950.00");
        assertThat(asientos.get(1).getCuentaCredito().getCodigoCuenta()).isEqualTo(CodigoCuentaContable.SOLCA_POR_PAGAR);
        assertThat(asientos.get(1).getMontoAsiento()).isEqualByComparingTo("50.00");
        assertThat(asientos.stream().map(AsientoContable::getMontoAsiento)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10000.00");
        Integer idOperacion = asientos.getFirst().getIdOperacion();
        assertThat(asientos).extracting(AsientoContable::getClaveIdempotencia)
                .containsExactly("DESEMBOLSO-" + idOperacion + "-NETO", "DESEMBOLSO-" + idOperacion + "-SOLCA");

        List<MovimientoContable> repetidos = asientos.stream().map(asiento -> new MovimientoContable(
                asiento.getIdOperacion(), asiento.getFechaAsiento(), asiento.getTipoAsiento(),
                asiento.getCuentaDebito().getCodigoCuenta(), asiento.getCuentaCredito().getCodigoCuenta(),
                asiento.getMontoAsiento(), asiento.getClaveIdempotencia())).toList();
        assertThat(servicioContabilidad.registrarAsientos(repetidos)).isZero();
        assertThat(asientoRepository.findByIdOperacionOrderByIdAsc(idOperacion))
                .extracting(AsientoContable::getId).containsExactlyElementsOf(asientos.stream().map(AsientoContable::getId).toList());
    }

    @Test
    void rechazaElSegundoDesembolsoSinCrearMasAsientos() {
        servicioPrestamo.disburse(idSolicitud, "2200000001");
        List<AsientoContable> registrados = asientosDesembolso();
        Integer operacion = registrados.getFirst().getIdOperacion();

        assertThatThrownBy(() -> servicioPrestamo.disburse(idSolicitud, "2200000001"))
                .hasMessageContaining("ya fue desembolsada");

        assertThat(asientoRepository.findByIdOperacionOrderByIdAsc(operacion))
                .extracting(AsientoContable::getId).containsExactlyElementsOf(registrados.stream().map(AsientoContable::getId).toList());
        verify(servicioPasivos).creditDisbursement(any());
    }

    @Test
    void revierteLaOperacionYElPrimerAsientoSiFallaElSegundo() {
        doAnswer(invocacion -> {
            List<MovimientoContable> movimientos = invocacion.getArgument(0);
            idOperacion = movimientos.getFirst().idOperacion();
            servicioContabilidad.registrarAsiento(movimientos.getFirst());
            entityManager.flush();
            assertThat(asientoRepository.findByIdOperacionOrderByIdAsc(idOperacion)).hasSize(1);
            throw new AsientoInvalidoException("Fallo al registrar SOLCA");
        }).when(servicioContabilidad).registrarAsientos(anyList());

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            assertThatThrownBy(() -> servicioPrestamo.disburse(idSolicitud, "2200000001"))
                    .isInstanceOf(AsientoInvalidoException.class);
            assertThat(estado.isRollbackOnly()).isTrue();
        });
        TestTransaction.end();
        assertThat(servicioPrestamo.findDisbursement(idSolicitud)).isEmpty();
        assertThat(idOperacion).isNotNull();
        assertThat(asientoRepository.findByIdOperacionOrderByIdAsc(idOperacion)).isEmpty();
        verify(servicioPasivos, never()).creditDisbursement(any());
    }

    @Test
    void revierteOperacionYAsientosCuandoPasivosFalla() {
        doAnswer(invocacion -> {
            idOperacion = entityManager.createQuery("select p.id from Prestamo p where p.idSolicitud = :solicitud", Integer.class)
                    .setParameter("solicitud", idSolicitud).getSingleResult();
            assertThat(asientoRepository.findByIdOperacionOrderByIdAsc(idOperacion)).hasSize(2);
            throw new IllegalStateException("Pasivos no disponible");
        }).when(servicioPasivos).creditDisbursement(any());

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            assertThatThrownBy(() -> servicioPrestamo.disburse(idSolicitud, "2200000001"))
                    .hasMessageContaining("No se pudo acreditar");
            assertThat(estado.isRollbackOnly()).isTrue();
        });

        TestTransaction.end();
        assertThat(servicioPrestamo.findDisbursement(idSolicitud)).isEmpty();
        assertThat(idOperacion).isNotNull();
        assertThat(asientoRepository.findByIdOperacionOrderByIdAsc(idOperacion)).isEmpty();
    }

    private List<AsientoContable> asientosDesembolso() {
        verify(servicioContabilidad).registrarAsientos(movimientosRegistrados.capture());
        Integer idOperacion = movimientosRegistrados.getValue().getFirst().idOperacion();
        return asientoRepository.findByIdOperacionOrderByIdAsc(idOperacion);
    }
}
