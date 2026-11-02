package com.grupob.bancobanquito.contabilidad.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.DatosProvision;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ResultadoProvisiones;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServiceProvisionesTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);

    @Test
    void registraElAumentoDeProvisionComoGastoYReserva() {
        ServiceContabilidad contabilidad = org.mockito.Mockito.mock(ServiceContabilidad.class);
        when(contabilidad.registrarAsientos(anyList())).thenReturn(1);
        ServiceProvisionesImpl servicio = new ServiceProvisionesImpl(contabilidad);

        ResultadoProvisiones resultado = servicio.registrarProvisiones(FECHA, List.of(
                new DatosProvision(7, new BigDecimal("1000.00"), new BigDecimal("0.0500"),
                        new BigDecimal("20.00"), "VENCIDA")));

        ArgumentCaptor<List<MovimientoContable>> captor = ArgumentCaptor.forClass(List.class);
        verify(contabilidad).registrarAsientos(captor.capture());
        MovimientoContable asiento = captor.getValue().get(0);
        assertThat(asiento.cuentaDebito()).isEqualTo(CodigoCuentaContable.GASTO_PROVISIONES);
        assertThat(asiento.cuentaCredito()).isEqualTo(CodigoCuentaContable.PROVISIONES_CARTERA);
        assertThat(asiento.monto()).isEqualByComparingTo("30.00");
        assertThat(resultado.provisionTotal()).isEqualByComparingTo("50.00");
        assertThat(resultado.variacionTotal()).isEqualByComparingTo("30.00");
    }

    @Test
    void registraLaDisminucionInvirtiendoElAsiento() {
        ServiceContabilidad contabilidad = org.mockito.Mockito.mock(ServiceContabilidad.class);
        when(contabilidad.registrarAsientos(anyList())).thenReturn(1);
        ServiceProvisionesImpl servicio = new ServiceProvisionesImpl(contabilidad);

        servicio.registrarProvisiones(FECHA, List.of(
                new DatosProvision(7, new BigDecimal("1000.00"), new BigDecimal("0.0100"),
                        new BigDecimal("50.00"), "POR_VENCER")));

        ArgumentCaptor<List<MovimientoContable>> captor = ArgumentCaptor.forClass(List.class);
        verify(contabilidad).registrarAsientos(captor.capture());
        MovimientoContable asiento = captor.getValue().get(0);
        assertThat(asiento.cuentaDebito()).isEqualTo(CodigoCuentaContable.PROVISIONES_CARTERA);
        assertThat(asiento.cuentaCredito()).isEqualTo(CodigoCuentaContable.GASTO_PROVISIONES);
        assertThat(asiento.monto()).isEqualByComparingTo("40.00");
    }
}
