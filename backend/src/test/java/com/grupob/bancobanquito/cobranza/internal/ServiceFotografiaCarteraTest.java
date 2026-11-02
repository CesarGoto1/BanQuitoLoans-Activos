package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ResumenFotoCartera;
import com.grupob.bancobanquito.cobranza.ResumenTramoCartera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceFotografiaCarteraTest {

    @Mock
    private OperacionCobranzaRepository operacionRepository;

    @Mock
    private CuotaCobranzaRepository cuotaRepository;

    @Mock
    private TramoCarteraRepository tramoRepository;

    @Mock
    private FotoCarteraRepository fotoRepository;

    @Mock
    private DevengoCobranzaRepository devengoRepository;

    private ServiceFotografiaCarteraImpl servicio;

    @BeforeEach
    void setUp() {
        this.servicio = new ServiceFotografiaCarteraImpl(
                operacionRepository,
                cuotaRepository,
                tramoRepository,
                fotoRepository,
                devengoRepository
        );
    }

    @Test
    @DisplayName("Debe registrar la fotografia diaria con calculo de provision segun el tramo")
    void debeTomarFotografiaConProvisionCalculada() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza operacion = crearOperacion(1, "OP-001", "POR_VENCER", new BigDecimal("10000.00"), 0);

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(operacion));

        TramoCartera tramo1 = crearTramo(1, 0, 0, "POR_VENCER", new BigDecimal("0.0100"));
        when(tramoRepository.findTramoPorDiasMora(0, fechaNegocio)).thenReturn(Optional.of(tramo1));

        when(devengoRepository.sumInteresDevengadoAcumulado(1, fechaNegocio)).thenReturn(new BigDecimal("59.98"));
        when(cuotaRepository.sumMoraPendientePorOperacion(1)).thenReturn(BigDecimal.ZERO);

        FotoCarteraId fotoId = new FotoCarteraId(1, fechaNegocio);
        when(fotoRepository.findById(fotoId)).thenReturn(Optional.empty());

        int total = servicio.tomarFotografiaDiaria(fechaNegocio);

        assertThat(total).isEqualTo(1);

        ArgumentCaptor<FotoCartera> captor = ArgumentCaptor.forClass(FotoCartera.class);
        verify(fotoRepository).save(captor.capture());

        FotoCartera foto = captor.getValue();
        assertThat(foto.getIdOperacion()).isEqualTo(1);
        assertThat(foto.getFechaFoto()).isEqualTo(fechaNegocio);
        assertThat(foto.getIdTramo()).isEqualTo(1);
        assertThat(foto.getEstadoCartera()).isEqualTo("POR_VENCER");
        assertThat(foto.getDiasMora()).isEqualTo(0);
        assertThat(foto.getSaldoCapital()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(foto.getValorProvision()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(foto.getInteresDevengadoAcumulado()).isEqualByComparingTo(new BigDecimal("59.98"));
        assertThat(foto.getInteresMoraAcumulado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Debe actualizar la fotografia existente si se reintenta el cierre en la misma fecha")
    void debeActualizarFotografiaSiYaExisteParaElDia() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza operacion = crearOperacion(2, "OP-002", "VENCIDA", new BigDecimal("8000.00"), 15);

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(operacion));

        TramoCartera tramo2 = crearTramo(2, 1, 30, "VENCIDA", new BigDecimal("0.0500"));
        when(tramoRepository.findTramoPorDiasMora(15, fechaNegocio)).thenReturn(Optional.of(tramo2));

        when(devengoRepository.sumInteresDevengadoAcumulado(2, fechaNegocio)).thenReturn(new BigDecimal("40.00"));
        when(cuotaRepository.sumMoraPendientePorOperacion(2)).thenReturn(new BigDecimal("5.50"));

        FotoCartera fotoPrevia = new FotoCartera();
        fotoPrevia.setIdOperacion(2);
        fotoPrevia.setFechaFoto(fechaNegocio);
        fotoPrevia.setSaldoCapital(new BigDecimal("9000.00"));

        FotoCarteraId fotoId = new FotoCarteraId(2, fechaNegocio);
        when(fotoRepository.findById(fotoId)).thenReturn(Optional.of(fotoPrevia));

        int total = servicio.tomarFotografiaDiaria(fechaNegocio);

        assertThat(total).isEqualTo(1);
        verify(fotoRepository).save(fotoPrevia);

        assertThat(fotoPrevia.getSaldoCapital()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(fotoPrevia.getValorProvision()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(fotoPrevia.getInteresMoraAcumulado()).isEqualByComparingTo(new BigDecimal("5.50"));
    }

    @Test
    @DisplayName("Debe omitir operaciones con estado CANCELADA en la fotografia de cartera")
    void debeOmitirOperacionesCanceladas() {
        LocalDate fechaNegocio = LocalDate.of(2026, 10, 31);
        OperacionCobranza cancelada = crearOperacion(3, "OP-003", "CANCELADA", BigDecimal.ZERO, 0);

        when(operacionRepository.findOperacionesActivas()).thenReturn(List.of(cancelada));

        int total = servicio.tomarFotografiaDiaria(fechaNegocio);

        assertThat(total).isEqualTo(0);
        verify(fotoRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Debe consultar fotografias de cartera por fecha de corte para reportes")
    void debeConsultarFotosPorFecha() {
        LocalDate fechaCorte = LocalDate.of(2026, 10, 31);
        FotoCartera f = new FotoCartera();
        f.setIdOperacion(1);
        f.setFechaFoto(fechaCorte);
        f.setIdTramo(1);
        f.setEstadoCartera("POR_VENCER");
        f.setDiasMora(0);
        f.setSaldoCapital(new BigDecimal("5000.00"));
        f.setValorProvision(new BigDecimal("50.00"));
        f.setInteresDevengadoAcumulado(new BigDecimal("25.00"));
        f.setInteresMoraAcumulado(BigDecimal.ZERO);

        when(fotoRepository.findByFechaFoto(fechaCorte)).thenReturn(List.of(f));

        List<ResumenFotoCartera> resultados = servicio.consultarFotosPorFecha(fechaCorte);

        assertThat(resultados).hasSize(1);
        ResumenFotoCartera r = resultados.getFirst();
        assertThat(r.idOperacion()).isEqualTo(1);
        assertThat(r.saldoCapital()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(r.valorProvision()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Debe consultar tramos vigentes para el modulo de reportes")
    void debeConsultarTramosVigentes() {
        LocalDate fechaCorte = LocalDate.of(2026, 10, 31);
        TramoCartera t = crearTramo(1, 0, 0, "POR_VENCER", new BigDecimal("0.0100"));

        when(tramoRepository.findTramosVigentes(fechaCorte)).thenReturn(List.of(t));

        List<ResumenTramoCartera> tramos = servicio.consultarTramosVigentes(fechaCorte);

        assertThat(tramos).hasSize(1);
        assertThat(tramos.getFirst().estadoCartera()).isEqualTo("POR_VENCER");
        assertThat(tramos.getFirst().porcentajeProvision()).isEqualByComparingTo(new BigDecimal("0.0100"));
    }

    private OperacionCobranza crearOperacion(Integer id, String numero, String estado, BigDecimal saldo, int diasMora) {
        OperacionCobranza op = new OperacionCobranza();
        op.setIdOperacion(id);
        op.setIdSolicitud(id);
        op.setIdCliente(100 + id);
        op.setCedulaCliente("171000000" + id);
        op.setSistemaAmortizacion("FRANCES");
        op.setNumeroOperacion(numero);
        op.setMontoAprobado(saldo);
        op.setTeaAsignada(new BigDecimal("0.1550"));
        op.setPlazoMeses(12);
        op.setSeguroMensual(BigDecimal.ZERO);
        op.setContribucionSolca(BigDecimal.ZERO);
        op.setMontoNeto(saldo);
        op.setCuentaDestino("2200000001");
        op.setFechaDesembolso(LocalDate.of(2026, 1, 1));
        op.setEstadoInstruccionDesembolso("ENTREGADA");
        op.setEstadoCartera(estado);
        op.setSaldoCapital(saldo);
        op.setDiasMora(diasMora);
        return op;
    }

    private TramoCartera crearTramo(Integer id, int desde, Integer hasta, String estado, BigDecimal provision) {
        TramoCartera t = new TramoCartera();
        t.setIdTramo(id);
        t.setNumeroTramo(id);
        t.setDiasMoraDesde(desde);
        t.setDiasMoraHasta(hasta);
        t.setEstadoCartera(estado);
        t.setDevengaIngresos(id <= 3);
        t.setTratamientoMora("SE_CALCULA");
        t.setPorcentajeProvision(provision);
        t.setFechaVigenciaDesde(LocalDate.of(2026, 1, 1));
        return t;
    }
}
