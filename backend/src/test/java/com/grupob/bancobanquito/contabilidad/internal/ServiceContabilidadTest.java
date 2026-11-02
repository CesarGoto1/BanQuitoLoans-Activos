package com.grupob.bancobanquito.contabilidad.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.contabilidad.AsientoInvalidoException;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ServiceContabilidadTest {

    private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 10, 31);

    @Autowired
    ServiceContabilidad servicioContabilidad;

    @Autowired
    AsientoContableRepository asientoRepository;

    @Autowired
    CuentaContableRepository cuentaRepository;

    @Test
    void elPlanDeCuentasTieneTodasLasCuentasDelEnum() {
        assertThat(Arrays.stream(CodigoCuentaContable.values())
                .allMatch(codigo -> cuentaRepository.findByCodigoCuenta(codigo).isPresent())).isTrue();
    }

    @Test
    void registraElAsientoConSusCuentas() {
        boolean registrado = servicioContabilidad.registrarAsiento(movimiento("PRUEBA-DESEMBOLSO-1",
                CodigoCuentaContable.CARTERA_POR_VENCER, CodigoCuentaContable.CUENTA_CLIENTE, "9950.00"));

        AsientoContable asiento = asientoRepository.findAll().stream()
                .filter(guardado -> guardado.getClaveIdempotencia().equals("PRUEBA-DESEMBOLSO-1"))
                .findFirst().orElseThrow();
        assertThat(registrado).isTrue();
        assertThat(asiento.getCuentaDebito().getCodigoCuenta()).isEqualTo(CodigoCuentaContable.CARTERA_POR_VENCER);
        assertThat(asiento.getCuentaCredito().getCodigoCuenta()).isEqualTo(CodigoCuentaContable.CUENTA_CLIENTE);
        assertThat(asiento.getMontoAsiento()).isEqualByComparingTo("9950.00");
        assertThat(asiento.getFechaAsiento()).isEqualTo(FECHA_NEGOCIO);
    }

    @Test
    void laMismaClaveNoDuplicaElAsiento() {
        MovimientoContable movimiento = movimiento("PRUEBA-REINTENTO-1",
                CodigoCuentaContable.INTERESES_POR_COBRAR, CodigoCuentaContable.INGRESOS_INTERESES, "3.42");
        long antes = asientoRepository.count();

        assertThat(servicioContabilidad.registrarAsiento(movimiento)).isTrue();
        assertThat(servicioContabilidad.registrarAsiento(movimiento)).isFalse();
        assertThat(asientoRepository.count()).isEqualTo(antes + 1);
    }

    @Test
    void registraVariosAsientosYCuentaSoloLosNuevos() {
        int registrados = servicioContabilidad.registrarAsientos(List.of(
                movimiento("PRUEBA-LOTE-1", CodigoCuentaContable.CARTERA_POR_VENCER,
                        CodigoCuentaContable.CUENTA_CLIENTE, "9950.00"),
                movimiento("PRUEBA-LOTE-2", CodigoCuentaContable.CARTERA_POR_VENCER,
                        CodigoCuentaContable.SOLCA_POR_PAGAR, "50.00"),
                movimiento("PRUEBA-LOTE-1", CodigoCuentaContable.CARTERA_POR_VENCER,
                        CodigoCuentaContable.CUENTA_CLIENTE, "9950.00")));

        assertThat(registrados).isEqualTo(2);
    }

    @Test
    void rechazaLaMismaCuentaEnDebitoYCredito() {
        assertThatThrownBy(() -> servicioContabilidad.registrarAsiento(movimiento("PRUEBA-MISMA-CUENTA",
                CodigoCuentaContable.CARTERA_POR_VENCER, CodigoCuentaContable.CARTERA_POR_VENCER, "10.00")))
                .isInstanceOf(AsientoInvalidoException.class);
    }

    @Test
    void rechazaMontosEnCeroOConMasDeDosDecimales() {
        assertThatThrownBy(() -> servicioContabilidad.registrarAsiento(movimiento("PRUEBA-CERO",
                CodigoCuentaContable.GASTO_PROVISIONES, CodigoCuentaContable.PROVISIONES_CARTERA, "0.00")))
                .isInstanceOf(AsientoInvalidoException.class);
        assertThatThrownBy(() -> servicioContabilidad.registrarAsiento(movimiento("PRUEBA-DECIMALES",
                CodigoCuentaContable.GASTO_PROVISIONES, CodigoCuentaContable.PROVISIONES_CARTERA, "1.005")))
                .isInstanceOf(AsientoInvalidoException.class);
    }

    @Test
    void rechazaUnaClaveVacia() {
        assertThatThrownBy(() -> servicioContabilidad.registrarAsiento(movimiento(" ",
                CodigoCuentaContable.GASTO_PROVISIONES, CodigoCuentaContable.PROVISIONES_CARTERA, "1.00")))
                .isInstanceOf(AsientoInvalidoException.class);
    }

    private MovimientoContable movimiento(String clave, CodigoCuentaContable debito, CodigoCuentaContable credito,
                                          String monto) {
        return new MovimientoContable(null, FECHA_NEGOCIO, TipoAsiento.DESEMBOLSO, debito, credito,
                new BigDecimal(monto), clave);
    }
}
