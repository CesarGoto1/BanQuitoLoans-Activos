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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Transactional
@Sql("/test-data-disbursement.sql")
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

    @BeforeEach
    void prepararDesembolso() {
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(FECHA);
        when(servicioPasivos.findCustomer(1)).thenReturn(Optional.of(
                new Cliente(1, "1710034065", "Pepito", "Perez")));
        when(servicioPasivos.findActiveAccounts(1)).thenReturn(List.of(new Cuenta("2200000001", "AHORROS")));
        assertThat(servicioPrestamo.findDisbursement(9001)).isEmpty();
    }

    @Test
    void persisteLosDosAsientosYNoLosDuplicaAlRepetirElLote() {
        long anteriores = asientoRepository.count();
        servicioPrestamo.disburse(9001, "2200000001");
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
        assertThat(asientoRepository.count()).isEqualTo(anteriores + 2);
    }

    @Test
    void rechazaElSegundoDesembolsoSinCrearMasAsientos() {
        servicioPrestamo.disburse(9001, "2200000001");
        long registrados = asientoRepository.count();

        assertThatThrownBy(() -> servicioPrestamo.disburse(9001, "2200000001"))
                .hasMessageContaining("ya fue desembolsada");

        assertThat(asientoRepository.count()).isEqualTo(registrados);
        verify(servicioPasivos).creditDisbursement(any());
    }

    @Test
    void revierteLaOperacionYElPrimerAsientoSiFallaElSegundo() {
        long anteriores = asientoRepository.count();
        doAnswer(invocacion -> {
            List<MovimientoContable> movimientos = invocacion.getArgument(0);
            servicioContabilidad.registrarAsiento(movimientos.getFirst());
            entityManager.flush();
            assertThat(asientoRepository.count()).isEqualTo(anteriores + 1);
            throw new AsientoInvalidoException("Fallo al registrar SOLCA");
        }).when(servicioContabilidad).registrarAsientos(anyList());

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            assertThatThrownBy(() -> servicioPrestamo.disburse(9001, "2200000001"))
                    .isInstanceOf(AsientoInvalidoException.class);
            assertThat(estado.isRollbackOnly()).isTrue();
        });
        TestTransaction.end();
        assertThat(servicioPrestamo.findDisbursement(9001)).isEmpty();
        assertThat(asientoRepository.count()).isEqualTo(anteriores);
        verify(servicioPasivos, never()).creditDisbursement(any());
    }

    @Test
    void revierteOperacionYAsientosCuandoPasivosFalla() {
        long anteriores = asientoRepository.count();
        doThrow(new IllegalStateException("Pasivos no disponible"))
                .when(servicioPasivos).creditDisbursement(any());

        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            assertThatThrownBy(() -> servicioPrestamo.disburse(9001, "2200000001"))
                    .hasMessageContaining("No se pudo acreditar");
            assertThat(estado.isRollbackOnly()).isTrue();
        });

        TestTransaction.end();
        assertThat(servicioPrestamo.findDisbursement(9001)).isEmpty();
        assertThat(asientoRepository.count()).isEqualTo(anteriores);
    }

    private List<AsientoContable> asientosDesembolso() {
        verify(servicioContabilidad).registrarAsientos(movimientosRegistrados.capture());
        Integer idOperacion = movimientosRegistrados.getValue().getFirst().idOperacion();
        return asientoRepository.findByIdOperacionOrderByIdAsc(idOperacion);
    }
}
