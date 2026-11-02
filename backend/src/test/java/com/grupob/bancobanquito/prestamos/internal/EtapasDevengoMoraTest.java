package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.prestamos.ServiceDevengoMora;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EtapasDevengoMoraTest {

    private static final LocalDate FECHA_NEGOCIO = LocalDate.of(2026, 11, 30);

    private final ServiceDevengoMora servicio = mock(ServiceDevengoMora.class);

    @Test
    void laEtapaDevengoEsElPaso2YSumaDevengosYAjustes() {
        when(servicio.ejecutarDevengo(FECHA_NEGOCIO)).thenReturn(5);
        when(servicio.ejecutarAjustesDevengo(FECHA_NEGOCIO)).thenReturn(2);
        EtapaDevengo etapa = new EtapaDevengo(servicio);

        assertThat(etapa.paso()).isEqualTo(PasoCierre.DEVENGO);
        assertThat(etapa.ejecutar(FECHA_NEGOCIO)).isEqualTo(7);
    }

    @Test
    void laEtapaMoraEsElPaso3() {
        when(servicio.ejecutarMora(FECHA_NEGOCIO)).thenReturn(3);
        EtapaMora etapa = new EtapaMora(servicio);

        assertThat(etapa.paso()).isEqualTo(PasoCierre.MORA);
        assertThat(etapa.ejecutar(FECHA_NEGOCIO)).isEqualTo(3);
    }
}
