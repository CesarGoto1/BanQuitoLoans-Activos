package com.grupob.bancobanquito.prestamos.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ConsultaDatosOperacionesTest {
    @Mock private PrestamoRepository prestamos;
    @Mock private CuotaRepository cuotas;
    @Mock private PagoRepository pagos;
    @Mock private CambioEstadoOperacionRepository cambios;
    @InjectMocks private ConsultaPrestamoServiceImpl servicio;

    @Test
    void consultaColectivaConservaDatosOriginalesYOrdenSolicitado() {
        var segundo = prestamo(2);
        segundo.setCustomerId(10);
        segundo.setApprovedAmount(new BigDecimal("12000.00"));
        segundo.setTermMonths(60);
        segundo.setAmortizationMethod(MetodoAmortizacion.ALEMAN);
        when(prestamos.findAllById(List.of(1, 2))).thenReturn(List.of(segundo, prestamo(1)));
        var datos = servicio.buscarDatosPorIds(List.of(1, 2));
        assertThat(datos).extracting(d -> d.idOperacion()).containsExactly(1, 2);
        assertThat(datos.getFirst().idCliente()).isEqualTo(10);
        assertThat(datos.getFirst().montoAprobado()).isEqualByComparingTo("2000.00");
        assertThat(datos.getFirst().plazoMeses()).isEqualTo(12);
        assertThat(datos.getFirst().sistemaAmortizacion()).isEqualTo(MetodoAmortizacion.FRANCES);
        assertThat(datos.getLast().montoAprobado()).isEqualByComparingTo("12000.00");
        assertThat(datos.getLast().plazoMeses()).isEqualTo(60);
        assertThat(datos.getLast().sistemaAmortizacion()).isEqualTo(MetodoAmortizacion.ALEMAN);
        verify(prestamos).findAllById(List.of(1, 2));
        verifyNoMoreInteractions(prestamos);
        verifyNoInteractions(cuotas, pagos, cambios);
    }

    @Test
    void rechazaIdentificadoresInvalidosODuplicadosSinConsultar() {
        assertThatThrownBy(() -> servicio.buscarDatosPorIds(List.of(1, 1))).isInstanceOf(ExcepcionPrestamo.class);
        assertThatThrownBy(() -> servicio.buscarDatosPorIds(Arrays.asList(1, null))).isInstanceOf(ExcepcionPrestamo.class);
        assertThatThrownBy(() -> servicio.buscarDatosPorIds(List.of(0))).isInstanceOf(ExcepcionPrestamo.class);
        assertThatThrownBy(() -> servicio.buscarDatosPorIds(null)).isInstanceOf(ExcepcionPrestamo.class);
        verifyNoInteractions(prestamos);
    }

    @Test
    void coleccionVaciaNoConsultaLaBase() {
        assertThat(servicio.buscarDatosPorIds(List.of())).isEmpty();
        verifyNoInteractions(prestamos);
    }

    @Test
    void rechazaOperacionesInexistentesSinDevolverResultadosParciales() {
        when(prestamos.findAllById(List.of(1, 2))).thenReturn(List.of(prestamo(1)));
        assertThatThrownBy(() -> servicio.buscarDatosPorIds(List.of(1, 2))).isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cliente", "monto", "negativo", "precision", "plazo", "cero", "sistema"})
    void rechazaDatosOriginalesIncompletosOInvalidos(String campo) {
        var operacion = prestamo(1);
        switch (campo) {
            case "cliente" -> operacion.setCustomerId(null);
            case "monto" -> operacion.setApprovedAmount(null);
            case "negativo" -> operacion.setApprovedAmount(new BigDecimal("-1"));
            case "precision" -> operacion.setApprovedAmount(new BigDecimal("10.001"));
            case "plazo" -> operacion.setTermMonths(null);
            case "cero" -> operacion.setTermMonths(0);
            case "sistema" -> operacion.setAmortizationMethod(null);
        }
        when(prestamos.findAllById(List.of(1))).thenReturn(List.of(operacion));
        assertThatThrownBy(() -> servicio.buscarDatosPorIds(List.of(1))).isInstanceOf(ExcepcionPrestamo.class)
                .extracting(e -> ((ExcepcionPrestamo) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rechazaFilasDuplicadasEnLaRespuesta() {
        when(prestamos.findAllById(List.of(1))).thenReturn(List.of(prestamo(1), prestamo(1)));
        assertThatThrownBy(() -> servicio.buscarDatosPorIds(List.of(1))).isInstanceOf(ExcepcionPrestamo.class);
    }

    private Prestamo prestamo(int id) {
        var prestamo = new Prestamo(id);
        prestamo.setCustomerId(10);
        prestamo.setApprovedAmount(new BigDecimal("2000.00"));
        prestamo.setTermMonths(12);
        prestamo.setAmortizationMethod(MetodoAmortizacion.FRANCES);
        return prestamo;
    }
}
