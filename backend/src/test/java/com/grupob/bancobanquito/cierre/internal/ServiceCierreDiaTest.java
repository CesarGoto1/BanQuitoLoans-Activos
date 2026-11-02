package com.grupob.bancobanquito.cierre.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grupob.bancobanquito.cierre.EjecucionEtapaCierre;
import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.EtapaCierreException;
import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.cierre.ResultadoCierreDia;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Transactional
class ServiceCierreDiaTest {

    private static final String ANALISTA = "1712345675";

    @Autowired
    ServiceCierreDiaImpl servicioCierre;

    @Autowired
    ServiceFechaNegocio servicioFechaNegocio;

    @Autowired
    CierreDiaRepository cierreRepository;

    @Autowired
    EjecucionEtapaRepository ejecucionRepository;

    @Autowired
    @Qualifier("etapaRecaudaciones")
    EtapaFalsa recaudaciones;

    @Autowired
    @Qualifier("etapaDevengo")
    EtapaFalsa devengo;

    @Autowired
    @Qualifier("etapaMora")
    EtapaFalsa mora;

    LocalDate hoy;

    @BeforeEach
    void prepararEtapas() {
        List.of(recaudaciones, devengo, mora).forEach(EtapaFalsa::reiniciar);
        hoy = servicioFechaNegocio.getCurrentBusinessDate();
    }

    @Test
    void ejecutaLasSeisEtapasEnOrdenYAvanzaLaFechaDeNegocio() {
        ResultadoCierreDia resultado = servicioCierre.ejecutarCierre(hoy, ANALISTA);

        assertThat(resultado.estadoCierre()).isEqualTo("COMPLETADO");
        assertThat(resultado.ejecuciones()).extracting(EjecucionEtapaCierre::etapa)
                .containsExactly(PasoCierre.values());
        assertThat(resultado.ejecuciones()).allSatisfy(ejecucion -> {
            assertThat(ejecucion.estadoEtapa()).isEqualTo("EXITOSA");
            assertThat(ejecucion.numeroIntento()).isEqualTo(1);
            assertThat(ejecucion.fechaHoraFin()).isNotNull();
        });
        assertThat(recaudaciones.fechas).containsExactly(hoy);
        assertThat(EtapaFalsa.orden).containsExactly(PasoCierre.RECAUDACIONES, PasoCierre.DEVENGO, PasoCierre.MORA);
        assertThat(servicioFechaNegocio.getCurrentBusinessDate()).isEqualTo(hoy.plusDays(1));
    }

    @Test
    void registraEnLaBitacoraLosRegistrosProcesadosDeCadaEtapa() {
        ResultadoCierreDia resultado = servicioCierre.ejecutarCierre(hoy, ANALISTA);

        assertThat(ejecucion(resultado, PasoCierre.RECAUDACIONES, 1).registrosProcesados()).isEqualTo(3);
        assertThat(ejecucion(resultado, PasoCierre.MORA, 1).duracionMilisegundos()).isNotNull();
    }

    @Test
    void lasEtapasSinImplementarSeRegistranSinProcesarRegistros() {
        ResultadoCierreDia resultado = servicioCierre.ejecutarCierre(hoy, ANALISTA);

        EjecucionEtapaCierre fotografia = ejecucion(resultado, PasoCierre.FOTOGRAFIA, 1);
        assertThat(fotografia.estadoEtapa()).isEqualTo("EXITOSA");
        assertThat(fotografia.registrosProcesados()).isZero();
    }

    @Test
    void noPermiteCerrarDosVecesElMismoDia() {
        servicioCierre.ejecutarCierre(hoy, ANALISTA);

        assertThatThrownBy(() -> servicioCierre.ejecutarCierre(hoy, ANALISTA))
                .isInstanceOfSatisfying(CierreException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessage("El día " + hoy + " ya fue cerrado.");
        assertThat(recaudaciones.fechas).containsExactly(hoy);
    }

    @Test
    void noPermiteCerrarUnaFechaDistintaALaVigente() {
        assertThatThrownBy(() -> servicioCierre.ejecutarCierre(hoy.plusDays(3), ANALISTA))
                .isInstanceOf(CierreException.class)
                .hasMessageStartingWith("La fecha de negocio vigente es " + hoy);
        assertThat(recaudaciones.fechas).isEmpty();
    }

    @Test
    void siUnaEtapaFallaSeDetieneYLaFechaNoAvanza() {
        mora.falla = true;

        ResultadoCierreDia resultado = servicioCierre.ejecutarCierre(hoy, ANALISTA);

        assertThat(resultado.estadoCierre()).isEqualTo("FALLIDO");
        assertThat(ejecucion(resultado, PasoCierre.MORA, 1).estadoEtapa()).isEqualTo("FALLIDA");
        assertThat(ejecucion(resultado, PasoCierre.MORA, 1).mensajeError()).isEqualTo("Falla simulada en MORA");
        assertThat(resultado.ejecuciones()).extracting(EjecucionEtapaCierre::etapa)
                .doesNotContain(PasoCierre.CLASIFICACION, PasoCierre.PROVISIONES, PasoCierre.FOTOGRAFIA);
        assertThat(servicioFechaNegocio.getCurrentBusinessDate()).isEqualTo(hoy);
    }

    @Test
    void unErrorTecnicoNoSeMuestraAlAnalista() {
        mora.fallaInesperada = true;

        ResultadoCierreDia resultado = servicioCierre.ejecutarCierre(hoy, ANALISTA);

        assertThat(ejecucion(resultado, PasoCierre.MORA, 1).mensajeError())
                .isEqualTo(ServiceCierreDiaImpl.ERROR_INESPERADO);
    }

    @Test
    void elReintentoContinuaDesdeLaEtapaFallidaSinRepetirLasExitosas() {
        mora.falla = true;
        servicioCierre.ejecutarCierre(hoy, ANALISTA);
        mora.falla = false;

        ResultadoCierreDia resultado = servicioCierre.ejecutarCierre(hoy, ANALISTA);

        assertThat(resultado.estadoCierre()).isEqualTo("COMPLETADO");
        assertThat(recaudaciones.fechas).hasSize(1);
        assertThat(devengo.fechas).hasSize(1);
        assertThat(mora.fechas).hasSize(2);
        assertThat(ejecucion(resultado, PasoCierre.MORA, 1).estadoEtapa()).isEqualTo("FALLIDA");
        assertThat(ejecucion(resultado, PasoCierre.MORA, 2).estadoEtapa()).isEqualTo("EXITOSA");
        assertThat(resultado.ejecuciones()).filteredOn(e -> e.etapa() == PasoCierre.RECAUDACIONES).hasSize(1);
        assertThat(servicioFechaNegocio.getCurrentBusinessDate()).isEqualTo(hoy.plusDays(1));
    }

    @Test
    void noPermiteCerrarUnDiaQueYaEstaEnEjecucion() {
        CierreDia cierre = cierreRepository.findByFechaNegocio(hoy).orElseThrow();
        cierre.cambiarEstado(EstadoCierre.EN_PROCESO, ANALISTA);

        assertThatThrownBy(() -> servicioCierre.ejecutarCierre(hoy, ANALISTA))
                .isInstanceOf(CierreException.class)
                .hasMessage("El cierre del día " + hoy + " ya está en ejecución.");
    }

    @Test
    void recuperaUnCierreInterrumpidoYPermiteReintentarlo() {
        CierreDia cierre = cierreRepository.findByFechaNegocio(hoy).orElseThrow();
        cierre.cambiarEstado(EstadoCierre.EN_PROCESO, ANALISTA);
        ejecucionRepository.save(EjecucionEtapa.iniciar(cierre.getId(), PasoCierre.RECAUDACIONES, 1, ANALISTA));

        assertThat(servicioCierre.recuperarCierresInterrumpidos()).isEqualTo(1);
        ResultadoCierreDia interrumpido = servicioCierre.consultarCierre(hoy);
        assertThat(interrumpido.estadoCierre()).isEqualTo("FALLIDO");
        assertThat(ejecucion(interrumpido, PasoCierre.RECAUDACIONES, 1).estadoEtapa()).isEqualTo("FALLIDA");

        ResultadoCierreDia resultado = servicioCierre.ejecutarCierre(hoy, ANALISTA);

        assertThat(resultado.estadoCierre()).isEqualTo("COMPLETADO");
        assertThat(ejecucion(resultado, PasoCierre.RECAUDACIONES, 2).estadoEtapa()).isEqualTo("EXITOSA");
    }

    @Test
    void consultarUnDiaSinCierreRespondeNoEncontrado() {
        assertThatThrownBy(() -> servicioCierre.consultarCierre(LocalDate.of(2020, 1, 1)))
                .isInstanceOfSatisfying(CierreException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static EjecucionEtapaCierre ejecucion(ResultadoCierreDia resultado, PasoCierre paso, int intento) {
        return resultado.ejecuciones().stream()
                .filter(ejecucion -> ejecucion.etapa() == paso && ejecucion.numeroIntento() == intento)
                .findFirst().orElseThrow();
    }

    @TestConfiguration
    static class EtapasDePrueba {

        @Bean
        EtapaFalsa etapaRecaudaciones() {
            return new EtapaFalsa(PasoCierre.RECAUDACIONES, 3);
        }

        @Bean
        EtapaFalsa etapaDevengo() {
            return new EtapaFalsa(PasoCierre.DEVENGO, 0);
        }

        @Bean
        EtapaFalsa etapaMora() {
            return new EtapaFalsa(PasoCierre.MORA, 0);
        }
    }

    static class EtapaFalsa implements EtapaCierre {

        static final List<PasoCierre> orden = new ArrayList<>();

        private final PasoCierre paso;
        private final int registros;
        final List<LocalDate> fechas = new ArrayList<>();
        boolean falla;
        boolean fallaInesperada;

        EtapaFalsa(PasoCierre paso, int registros) {
            this.paso = paso;
            this.registros = registros;
        }

        void reiniciar() {
            orden.clear();
            fechas.clear();
            falla = false;
            fallaInesperada = false;
        }

        @Override
        public PasoCierre paso() {
            return paso;
        }

        @Override
        public int ejecutar(LocalDate fechaNegocio) {
            orden.add(paso);
            fechas.add(fechaNegocio);
            if (falla) {
                throw new EtapaCierreException("Falla simulada en " + paso);
            }
            if (fallaInesperada) {
                throw new IllegalStateException("could not execute statement [ERROR: deadlock detected]");
            }
            return registros;
        }
    }
}
