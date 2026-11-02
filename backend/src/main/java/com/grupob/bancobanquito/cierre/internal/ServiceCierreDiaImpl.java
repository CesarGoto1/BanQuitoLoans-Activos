package com.grupob.bancobanquito.cierre.internal;

import com.grupob.bancobanquito.cierre.EjecucionEtapaCierre;
import com.grupob.bancobanquito.cierre.EtapaCierre;
import com.grupob.bancobanquito.cierre.EtapaCierreException;
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
    static final String ERROR_INESPERADO =
            "Ocurrió un error inesperado en la etapa. Comuníquelo a soporte técnico y reintente el cierre.";

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
        Set<PasoCierre> exitosas = etapasExitosas(cierre.getId());
        for (PasoCierre paso : PasoCierre.values()) {
            if (exitosas.contains(paso)) {
                continue;
            }
            if (!ejecutarEtapa(cierre, paso, usuario)) {
                transaccion.executeWithoutResult(estado -> cambiarEstado(cierre.getId(), EstadoCierre.FALLIDO, usuario));
                return consultarCierre(fechaNegocio);
            }
        }
        transaccion.executeWithoutResult(estado -> completar(cierre.getId(), usuario));
        return consultarCierre(fechaNegocio);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoCierreDia consultarCierre(LocalDate fechaNegocio) {
        return cierreRepository.findByFechaNegocio(fechaNegocio)
                .map(this::aResultado)
                .orElseThrow(() -> new CierreException(HttpStatus.NOT_FOUND,
                        "No existe un cierre para el día " + fechaNegocio + "."));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResultadoCierreDia> listarCierres(Pageable paginacion) {
        return cierreRepository.findAll(paginacion).map(this::aResultado);
    }

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
                .orElseThrow(() -> new CierreException(HttpStatus.CONFLICT, "No hay una fecha de negocio abierta."));
        if (!vigente.getFechaNegocio().equals(fechaNegocio)) {
            if (cierreRepository.existsByFechaNegocioAndEstadoCierre(fechaNegocio, EstadoCierre.COMPLETADO)) {
                throw new CierreException(HttpStatus.CONFLICT, "El día " + fechaNegocio + " ya fue cerrado.");
            }
            throw new CierreException(HttpStatus.CONFLICT, "La fecha de negocio vigente es "
                    + vigente.getFechaNegocio() + "; no se puede cerrar el " + fechaNegocio + ".");
        }
        CierreDia cierre = cierreRepository.findForClosing(fechaNegocio).orElseThrow();
        if (cierre.getEstadoCierre() == EstadoCierre.EN_PROCESO) {
            throw new CierreException(HttpStatus.CONFLICT, "El cierre del día " + fechaNegocio + " ya está en ejecución.");
        }
        cierre.cambiarEstado(EstadoCierre.EN_PROCESO, usuario);
        return cierre;
    }

    private boolean ejecutarEtapa(CierreDia cierre, PasoCierre paso, String usuario) {
        EjecucionEtapa ejecucion = transaccion.execute(estado -> ejecucionRepository.save(
                EjecucionEtapa.iniciar(cierre.getId(), paso, siguienteIntento(cierre.getId(), paso), usuario)));
        EtapaCierre etapa = etapas.get(paso);
        try {
            transaccion.executeWithoutResult(estado -> {
                ejecucion.terminar(etapa == null ? 0 : etapa.ejecutar(cierre.getFechaNegocio()), usuario);
                ejecucionRepository.save(ejecucion);
            });
            return true;
        } catch (RuntimeException e) {
            String mensaje = e instanceof EtapaCierreException ? e.getMessage() : ERROR_INESPERADO;
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

}
