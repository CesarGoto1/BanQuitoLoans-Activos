package com.grupob.bancobanquito.cierre.internal;

import com.grupob.bancobanquito.cierre.EjecucionEtapaCierre;
import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.PasoCierre;
import com.grupob.bancobanquito.cierre.ResultadoCierreDia;
import com.grupob.bancobanquito.cierre.ServiceCierreDia;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServiceCierreDiaImpl implements ServiceCierreDia {

    static final String USUARIO_SISTEMA = "sistema";

    private static final Logger log = LoggerFactory.getLogger(ServiceCierreDiaImpl.class);

    private final CierreDiaRepository cierreRepository;
    private final EjecucionEtapaRepository ejecucionRepository;
    private final Map<PasoCierre, EtapaCierre> etapas;
    private final TransactionTemplate transaccion;

    ServiceCierreDiaImpl(CierreDiaRepository cierreRepository, EjecucionEtapaRepository ejecucionRepository,
            List<EtapaCierre> etapas, PlatformTransactionManager transactionManager) {
        this.cierreRepository = cierreRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.etapas = indexar(etapas);
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @Override
    public ResultadoCierreDia ejecutarCierre(LocalDate fechaNegocio, String usuario) {
        CierreDia cierre = transaccion.execute(estado -> iniciar(fechaNegocio, usuario));
        log.info("Cierre del {} iniciado por {}", fechaNegocio, enmascarar(usuario));
        Set<PasoCierre> exitosas = etapasExitosas(cierre.getId());
        for (PasoCierre paso : PasoCierre.values()) {
            if (exitosas.contains(paso)) {
                continue;
            }
            if (!ejecutarEtapa(cierre, paso, usuario)) {
                transaccion.executeWithoutResult(estado -> cambiarEstado(cierre.getId(), EstadoCierre.FALLIDO, usuario));
                log.warn("Cierre del {} detenido en la etapa {}; la fecha de negocio no avanza", fechaNegocio, paso);
                return consultarCierre(fechaNegocio);
            }
        }
        transaccion.executeWithoutResult(estado -> completar(cierre.getId(), usuario));
        log.info("Cierre del {} completado; nueva fecha de negocio {}", fechaNegocio, fechaNegocio.plusDays(1));
        return consultarCierre(fechaNegocio);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoCierreDia consultarCierre(LocalDate fechaNegocio) {
        return cierreRepository.findByFechaNegocio(fechaNegocio)
                .map(this::aResultado)
                .orElseThrow(() -> new ExcepcionCierre(HttpStatus.NOT_FOUND,
                        "No existe un cierre para el día " + fechaNegocio + "."));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResultadoCierreDia> listarCierres(Pageable paginacion) {
        return cierreRepository.findAll(paginacion).map(this::aResultado);
    }

    // Un cierre que quedó EN_PROCESO solo puede venir de una caída de la aplicación a mitad de una etapa.
    @Transactional
    public int recuperarCierresInterrumpidos() {
        ejecucionRepository.findByEstadoEtapa(EstadoEtapa.EN_PROCESO).forEach(ejecucion -> ejecucion.fallar(
                "La etapa se interrumpió antes de terminar; vuelva a ejecutar el cierre.", USUARIO_SISTEMA));
        List<CierreDia> interrumpidos = cierreRepository.findByEstadoCierre(EstadoCierre.EN_PROCESO);
        interrumpidos.forEach(cierre -> cierre.cambiarEstado(EstadoCierre.FALLIDO, USUARIO_SISTEMA));
        return interrumpidos.size();
    }

    private CierreDia iniciar(LocalDate fechaNegocio, String usuario) {
        CierreDia vigente = cierreRepository.findFirstByEstadoCierreNotOrderByFechaNegocioAsc(EstadoCierre.COMPLETADO)
                .orElseThrow(() -> new ExcepcionCierre(HttpStatus.CONFLICT, "No hay una fecha de negocio abierta."));
        if (!vigente.getFechaNegocio().equals(fechaNegocio)) {
            if (cierreRepository.existsByFechaNegocioAndEstadoCierre(fechaNegocio, EstadoCierre.COMPLETADO)) {
                throw new ExcepcionCierre(HttpStatus.CONFLICT, "El día " + fechaNegocio + " ya fue cerrado.");
            }
            throw new ExcepcionCierre(HttpStatus.CONFLICT, "La fecha de negocio vigente es "
                    + vigente.getFechaNegocio() + "; no se puede cerrar el " + fechaNegocio + ".");
        }
        CierreDia cierre = cierreRepository.findForClosing(fechaNegocio).orElseThrow();
        if (cierre.getEstadoCierre() == EstadoCierre.EN_PROCESO) {
            throw new ExcepcionCierre(HttpStatus.CONFLICT, "El cierre del día " + fechaNegocio + " ya está en ejecución.");
        }
        cierre.cambiarEstado(EstadoCierre.EN_PROCESO, usuario);
        return cierre;
    }

    private boolean ejecutarEtapa(CierreDia cierre, PasoCierre paso, String usuario) {
        EjecucionEtapa ejecucion = transaccion.execute(estado -> ejecucionRepository.save(
                EjecucionEtapa.iniciar(cierre.getId(), paso, siguienteIntento(cierre.getId(), paso), usuario)));
        EtapaCierre etapa = etapas.get(paso);
        if (etapa == null) {
            log.warn("La etapa {} no tiene implementación; se registra sin procesar registros", paso);
        }
        try {
            transaccion.executeWithoutResult(estado -> {
                ejecucion.terminar(etapa == null ? 0 : etapa.ejecutar(cierre.getFechaNegocio()), usuario);
                ejecucionRepository.save(ejecucion);
            });
            return true;
        } catch (RuntimeException e) {
            log.error("Falló la etapa {} del cierre del {}", paso, cierre.getFechaNegocio(), e);
            String mensaje = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            transaccion.executeWithoutResult(estado -> {
                ejecucion.fallar(mensaje, usuario);
                ejecucionRepository.save(ejecucion);
            });
            return false;
        }
    }

    private void completar(Integer idCierre, String usuario) {
        CierreDia cierre = cambiarEstado(idCierre, EstadoCierre.COMPLETADO, usuario);
        LocalDate siguiente = cierre.getFechaNegocio().plusDays(1);
        if (!cierreRepository.existsByFechaNegocio(siguiente)) {
            cierreRepository.save(CierreDia.abrir(siguiente, usuario));
        }
    }

    private CierreDia cambiarEstado(Integer idCierre, EstadoCierre estadoCierre, String usuario) {
        CierreDia cierre = cierreRepository.findById(idCierre).orElseThrow();
        cierre.cambiarEstado(estadoCierre, usuario);
        return cierre;
    }

    private Set<PasoCierre> etapasExitosas(Integer idCierre) {
        Set<PasoCierre> exitosas = EnumSet.noneOf(PasoCierre.class);
        ejecucionRepository.findByIdCierreOrderByNumeroEtapaAscNumeroIntentoAsc(idCierre).stream()
                .filter(ejecucion -> ejecucion.getEstadoEtapa() == EstadoEtapa.EXITOSA)
                .forEach(ejecucion -> exitosas.add(paso(ejecucion.getNumeroEtapa())));
        return exitosas;
    }

    private int siguienteIntento(Integer idCierre, PasoCierre paso) {
        return ejecucionRepository.findByIdCierreOrderByNumeroEtapaAscNumeroIntentoAsc(idCierre).stream()
                .filter(ejecucion -> ejecucion.getNumeroEtapa() == paso.numeroEtapa())
                .mapToInt(EjecucionEtapa::getNumeroIntento)
                .max().orElse(0) + 1;
    }

    private ResultadoCierreDia aResultado(CierreDia cierre) {
        List<EjecucionEtapaCierre> ejecuciones = ejecucionRepository
                .findByIdCierreOrderByNumeroEtapaAscNumeroIntentoAsc(cierre.getId()).stream()
                .map(ejecucion -> new EjecucionEtapaCierre(
                        ejecucion.getNumeroEtapa(),
                        paso(ejecucion.getNumeroEtapa()),
                        ejecucion.getNumeroIntento(),
                        ejecucion.getEstadoEtapa().name(),
                        ejecucion.getFechaHoraInicio(),
                        ejecucion.getFechaHoraFin(),
                        ejecucion.getFechaHoraFin() == null ? null
                                : Duration.between(ejecucion.getFechaHoraInicio(), ejecucion.getFechaHoraFin()).toMillis(),
                        ejecucion.getRegistrosProcesados(),
                        ejecucion.getMensajeError()))
                .toList();
        return new ResultadoCierreDia(cierre.getFechaNegocio(), cierre.getEstadoCierre().name(), ejecuciones);
    }

    private static Map<PasoCierre, EtapaCierre> indexar(List<EtapaCierre> etapas) {
        Map<PasoCierre, EtapaCierre> porPaso = new EnumMap<>(PasoCierre.class);
        for (EtapaCierre etapa : etapas) {
            EtapaCierre repetida = porPaso.put(etapa.paso(), etapa);
            if (repetida != null) {
                throw new IllegalStateException("La etapa " + etapa.paso() + " tiene dos implementaciones: "
                        + repetida.getClass().getSimpleName() + " y " + etapa.getClass().getSimpleName() + ".");
            }
        }
        return porPaso;
    }

    private static PasoCierre paso(int numeroEtapa) {
        for (PasoCierre paso : PasoCierre.values()) {
            if (paso.numeroEtapa() == numeroEtapa) {
                return paso;
            }
        }
        throw new IllegalStateException("No existe la etapa " + numeroEtapa + ".");
    }

    private static String enmascarar(String usuario) {
        if (usuario == null || usuario.length() <= 4 || !usuario.matches("[0-9]+")) {
            return usuario;
        }
        return usuario.substring(0, 2) + "******" + usuario.substring(usuario.length() - 2);
    }
}
