package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.SolicitudBandejaResponse;
import com.grupob.bancobanquito.origination.SolicitudDetalleResponse;
import com.grupob.bancobanquito.parameters.ParameterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceCapacidadPagoTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private ParameterService parameterService;

    private SolicitudServiceImpl solicitudService;

    @BeforeEach
    void setUp() {
        this.solicitudService = new SolicitudServiceImpl(solicitudRepository, parameterService);
    }

    @Test
    @DisplayName("Debe aprobar capacidad de pago cuando la cuota referencial representa <= 40% del remanente")
    void debeAprobarCapacidadDePagoDentroDelLimite() {
        Solicitud solicitud = crearSolicitudBase(1, new BigDecimal("1500.00"), new BigDecimal("500.00"), new BigDecimal("300.00"));
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(solicitud));
        when(parameterService.getValue("PAR-09")).thenReturn(new BigDecimal("0.4000"));

        SolicitudDetalleResponse detalle = solicitudService.obtenerDetalleParaEvaluacion(1);

        assertThat(detalle.ingresoRemanente()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(detalle.cuotaEvaluacionReferencial()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(detalle.porcentajeImpactoCuota()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(detalle.limiteImpactoMaximo()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(detalle.cuotaMaximaAdmisible()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(detalle.capacidadPagoValida()).isTrue();
        assertThat(detalle.mensajeCapacidadPago()).contains("dentro del límite regulatorio");
    }

    @Test
    @DisplayName("Debe reprobar capacidad de pago cuando la cuota supera el 40% del remanente (RN-09)")
    void debeReprobarCapacidadDePagoCuandoSuperaCuarentaPorCiento() {
        Solicitud solicitud = crearSolicitudBase(2, new BigDecimal("1200.00"), new BigDecimal("400.00"), new BigDecimal("400.00"));
        when(solicitudRepository.findById(2)).thenReturn(Optional.of(solicitud));
        when(parameterService.getValue("PAR-09")).thenReturn(new BigDecimal("0.4000"));

        SolicitudDetalleResponse detalle = solicitudService.obtenerDetalleParaEvaluacion(2);

        assertThat(detalle.ingresoRemanente()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(detalle.cuotaEvaluacionReferencial()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(detalle.porcentajeImpactoCuota()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(detalle.cuotaMaximaAdmisible()).isEqualByComparingTo(new BigDecimal("320.00"));
        assertThat(detalle.capacidadPagoValida()).isFalse();
        assertThat(detalle.mensajeCapacidadPago()).contains("ALERTA: La cuota referencial");
    }

    @Test
    @DisplayName("Debe reprobar capacidad de pago si el remanente mensual es cero o negativo (RN-10)")
    void debeReprobarCuandoRemanenteEsNegativo() {
        Solicitud solicitud = crearSolicitudBase(3, new BigDecimal("800.00"), new BigDecimal("1000.00"), new BigDecimal("150.00"));
        when(solicitudRepository.findById(3)).thenReturn(Optional.of(solicitud));
        when(parameterService.getValue("PAR-09")).thenReturn(new BigDecimal("0.4000"));

        SolicitudDetalleResponse detalle = solicitudService.obtenerDetalleParaEvaluacion(3);

        assertThat(detalle.ingresoRemanente()).isEqualByComparingTo(new BigDecimal("-200.00"));
        assertThat(detalle.capacidadPagoValida()).isFalse();
        assertThat(detalle.mensajeCapacidadPago()).contains("no dispone de remanente mensual positivo (RN-10)");
    }

    @Test
    @DisplayName("Debe listar bandeja de solicitudes ordenadas por antigüedad ascendente (RF-04)")
    void debeListarBandejaPorAntiguedad() {
        Solicitud sol1 = crearSolicitudBase(10, new BigDecimal("2000.00"), new BigDecimal("800.00"), new BigDecimal("250.00"));
        sol1.setFechaHoraRegistro(LocalDateTime.of(2026, 10, 20, 9, 0));

        Solicitud sol2 = crearSolicitudBase(11, new BigDecimal("1800.00"), new BigDecimal("700.00"), new BigDecimal("220.00"));
        sol2.setFechaHoraRegistro(LocalDateTime.of(2026, 10, 22, 14, 30));

        when(solicitudRepository.findByEstadoSolicitudOrderByFechaHoraRegistroAsc(EstadoSolicitud.REGISTRADA))
                .thenReturn(List.of(sol1, sol2));

        List<SolicitudBandejaResponse> bandeja = solicitudService.listarBandejaAnalista("REGISTRADA");

        assertThat(bandeja).hasSize(2);
        assertThat(bandeja.get(0).idSolicitud()).isEqualTo(10);
        assertThat(bandeja.get(1).idSolicitud()).isEqualTo(11);
    }

    @Test
    void rejectsOneCentOverCapacityEvenWhenDisplayedPercentageIsForty() {
        Solicitud application = crearSolicitudBase(20, new BigDecimal("1500.00"),
                new BigDecimal("500.00"), new BigDecimal("400.01"));
        when(solicitudRepository.findById(20)).thenReturn(Optional.of(application));
        when(parameterService.getValue("PAR-09")).thenReturn(new BigDecimal("0.4000"));

        SolicitudDetalleResponse detail = solicitudService.obtenerDetalleParaEvaluacion(20);

        assertThat(detail.porcentajeImpactoCuota()).isEqualByComparingTo("40.00");
        assertThat(detail.capacidadPagoValida()).isFalse();
    }

    @Test
    @DisplayName("Debe lanzar SolicitudNoEncontradaException cuando el ID no existe")
    void debeLanzarExcepcionCuandoNoExiste() {
        when(solicitudRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.obtenerDetalleParaEvaluacion(999))
                .isInstanceOf(SolicitudNoEncontradaException.class)
                .hasMessageContaining("999");
    }

    private Solicitud crearSolicitudBase(Integer id, BigDecimal ingresos, BigDecimal gastos, BigDecimal cuotaRef) {
        Solicitud s = new Solicitud();
        s.setIdSolicitud(id);
        s.setIdCliente(101);
        s.setSistemaAmortizacion("FRANCES");
        s.setMontoSolicitado(new BigDecimal("5000.00"));
        s.setPlazoMeses(12);
        s.setDestinoCredito("EDUCACION");
        s.setIngresosMensuales(ingresos);
        s.setGastosMensuales(gastos);
        s.setFechaSolicitud(LocalDate.of(2026, 10, 24));
        s.setFechaHoraRegistro(LocalDateTime.of(2026, 10, 24, 10, 0));
        s.setCuotaEvaluacionReferencial(cuotaRef);
        s.setEstadoSolicitud(EstadoSolicitud.REGISTRADA);
        s.setUsuarioCreacion("cliente_web");
        s.setFechaCreacion(LocalDateTime.of(2026, 10, 24, 10, 0));
        return s;
    }
}
