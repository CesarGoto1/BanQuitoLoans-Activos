package com.grupob.bancobanquito.contabilidad.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceCuadreContableTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 31);

    @Mock
    AsientoContableRepository asientoRepository;
    @Mock
    ServiceFechaNegocio servicioFechaNegocio;
    @InjectMocks
    ServiceCuadreContableImpl servicioCuadre;

    @Test
    void sumaNetoYSolcaSinDuplicarLaCantidadDeAsientos() {
        when(asientoRepository.findByFechaAsientoOrderByIdAsc(FECHA))
                .thenReturn(List.of(asiento("9950.00"), asiento("50.00")));

        var resultado = servicioCuadre.consultarCuadre(FECHA);

        assertThat(resultado.fechaNegocio()).isEqualTo(FECHA);
        assertThat(resultado.totalDebitos()).isEqualByComparingTo("10000.00");
        assertThat(resultado.totalCreditos()).isEqualByComparingTo("10000.00");
        assertThat(resultado.diferencia()).isEqualByComparingTo("0.00");
        assertThat(resultado.cantidadAsientos()).isEqualTo(2);
        assertThat(resultado.cuadrado()).isTrue();
        assertThat(resultado.observacion()).contains("no verifica que todos los eventos");
        verify(asientoRepository).findByFechaAsientoOrderByIdAsc(FECHA);
        verifyNoMoreInteractions(asientoRepository);
        verifyNoInteractions(servicioFechaNegocio);
    }

    @Test
    void distingueUnaFechaSinAsientosDeUnaContabilizacionCompleta() {
        when(asientoRepository.findByFechaAsientoOrderByIdAsc(FECHA)).thenReturn(List.of());

        var resultado = servicioCuadre.consultarCuadre(FECHA);

        assertThat(resultado.cantidadAsientos()).isZero();
        assertThat(resultado.totalDebitos()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resultado.totalCreditos()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resultado.diferencia()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resultado.cuadrado()).isTrue();
        assertThat(resultado.observacion()).contains("No hay asientos registrados", "no verifica");
    }

    @Test
    void conservaLosCentavosYPermiteTotalesSuperioresAlLimiteDeUnAsiento() {
        when(asientoRepository.findByFechaAsientoOrderByIdAsc(FECHA)).thenReturn(List.of(
                asiento("99999999.99"), asiento("99999999.99"), asiento("0.01"), asiento("0.02")));

        var resultado = servicioCuadre.consultarCuadre(FECHA);

        assertThat(resultado.totalDebitos()).isEqualTo(new BigDecimal("200000000.01"));
        assertThat(resultado.totalCreditos()).isEqualTo(new BigDecimal("200000000.01"));
        assertThat(resultado.diferencia()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resultado.cantidadAsientos()).isEqualTo(4);
    }

    @Test
    void consultaLaFechaVigenteDelServicioDeNegocio() {
        when(servicioFechaNegocio.getCurrentBusinessDate()).thenReturn(FECHA);
        when(asientoRepository.findByFechaAsientoOrderByIdAsc(FECHA)).thenReturn(List.of(asiento("25.12")));

        var resultado = servicioCuadre.consultarCuadreActual();

        assertThat(resultado.fechaNegocio()).isEqualTo(FECHA);
        assertThat(resultado.totalDebitos()).isEqualByComparingTo("25.12");
        verify(servicioFechaNegocio).getCurrentBusinessDate();
        verify(asientoRepository).findByFechaAsientoOrderByIdAsc(FECHA);
    }

    @Test
    void rechazaLaFechaNulaSinConsultarElRepositorio() {
        assertThatThrownBy(() -> servicioCuadre.consultarCuadre(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de negocio es obligatoria.");

        verifyNoInteractions(asientoRepository, servicioFechaNegocio);
    }

    @Test
    void propagaLaAusenciaDeUnaFechaDeNegocioAbierta() {
        when(servicioFechaNegocio.getCurrentBusinessDate())
                .thenThrow(new IllegalStateException("No hay una fecha de negocio abierta."));

        assertThatThrownBy(() -> servicioCuadre.consultarCuadreActual())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No hay una fecha de negocio abierta.");

        verifyNoInteractions(asientoRepository);
    }

    @Test
    void unFalloDeConsultaNoSePresentaComoUnDiaSinMovimientos() {
        when(asientoRepository.findByFechaAsientoOrderByIdAsc(FECHA))
                .thenThrow(new IllegalStateException("No se pudo consultar la contabilidad"));

        assertThatThrownBy(() -> servicioCuadre.consultarCuadre(FECHA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo consultar la contabilidad");
    }

    private AsientoContable asiento(String monto) {
        AsientoContable asiento = new AsientoContable();
        asiento.setFechaAsiento(FECHA);
        asiento.setMontoAsiento(new BigDecimal(monto));
        return asiento;
    }
}
