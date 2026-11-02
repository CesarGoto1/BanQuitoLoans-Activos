package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ServiceProvisionesCartera;
import com.grupob.bancobanquito.contabilidad.DatosProvision;
import com.grupob.bancobanquito.contabilidad.ServiceProvisiones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceProvisionesCarteraImpl implements ServiceProvisionesCartera {

    private final OperacionCobranzaRepository operacionRepository;
    private final TramoCarteraRepository tramoRepository;
    private final FotoCarteraRepository fotoRepository;
    private final ServiceProvisiones servicioProvisiones;

    public ServiceProvisionesCarteraImpl(
            OperacionCobranzaRepository operacionRepository,
            TramoCarteraRepository tramoRepository,
            FotoCarteraRepository fotoRepository,
            ServiceProvisiones servicioProvisiones) {
        this.operacionRepository = operacionRepository;
        this.tramoRepository = tramoRepository;
        this.fotoRepository = fotoRepository;
        this.servicioProvisiones = servicioProvisiones;
    }

    @Override
    @Transactional
    public int provisionarCartera(LocalDate fechaNegocio) {
        List<DatosProvision> datos = new ArrayList<>();
        for (OperacionCobranza operacion : this.operacionRepository.findOperacionesActivas()) {
            datos.add(datosProvision(operacion, fechaNegocio));
        }
        return this.servicioProvisiones.registrarProvisiones(fechaNegocio, datos).operacionesProcesadas();
    }

    private DatosProvision datosProvision(OperacionCobranza operacion, LocalDate fechaNegocio) {
        int diasMora = Math.max(0, operacion.getDiasMora() != null ? operacion.getDiasMora() : 0);
        TramoCartera tramo = this.tramoRepository.findTramoPorDiasMora(diasMora, fechaNegocio)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro tramo para " + diasMora + " dias de mora en " + fechaNegocio));
        BigDecimal saldoCapital = (operacion.getSaldoCapital() != null ? operacion.getSaldoCapital() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal provisionAnterior = this.fotoRepository
                .findFirstByIdOperacionAndFechaFotoBeforeOrderByFechaFotoDesc(operacion.getIdOperacion(), fechaNegocio)
                .map(FotoCartera::getValorProvision)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return new DatosProvision(operacion.getIdOperacion(), saldoCapital, tramo.getPorcentajeProvision(),
                provisionAnterior, operacion.getEstadoCartera());
    }
}
