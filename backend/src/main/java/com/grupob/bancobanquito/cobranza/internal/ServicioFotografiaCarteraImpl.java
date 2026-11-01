package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ServicioFotografiaCartera;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ServicioFotografiaCarteraImpl implements ServicioFotografiaCartera {

    private final OperacionCobranzaRepository operacionRepository;
    private final CuotaCobranzaRepository cuotaRepository;
    private final TramoCarteraRepository tramoRepository;
    private final FotoCarteraRepository fotoRepository;
    private final DevengoCobranzaRepository devengoRepository;

    public ServicioFotografiaCarteraImpl(
            OperacionCobranzaRepository operacionRepository,
            CuotaCobranzaRepository cuotaRepository,
            TramoCarteraRepository tramoRepository,
            FotoCarteraRepository fotoRepository,
            DevengoCobranzaRepository devengoRepository) {
        this.operacionRepository = operacionRepository;
        this.cuotaRepository = cuotaRepository;
        this.tramoRepository = tramoRepository;
        this.fotoRepository = fotoRepository;
        this.devengoRepository = devengoRepository;
    }

    @Override
    public int tomarFotografiaDiaria(LocalDate fechaNegocio) {
        List<OperacionCobranza> operaciones = this.operacionRepository.findOperacionesActivas();
        int procesadas = 0;

        for (OperacionCobranza operacion : operaciones) {
            if ("CANCELADA".equalsIgnoreCase(operacion.getEstadoCartera())) {
                continue;
            }
            procesarFotoOperacion(operacion, fechaNegocio);
            procesadas++;
        }

        return procesadas;
    }

    private void procesarFotoOperacion(OperacionCobranza operacion, LocalDate fechaNegocio) {
        int diasMora = Math.max(0, operacion.getDiasMora() != null ? operacion.getDiasMora() : 0);

        TramoCartera tramo = this.tramoRepository.findTramoPorDiasMora(diasMora, fechaNegocio)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro tramo para " + diasMora + " dias de mora en " + fechaNegocio));

        BigDecimal saldoCapital = operacion.getSaldoCapital() != null
                ? operacion.getSaldoCapital().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal valorProvision = saldoCapital.multiply(tramo.getPorcentajeProvision())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal interesDevengado = calcularInteresDevengado(operacion.getIdOperacion(), fechaNegocio);
        BigDecimal interesMora = calcularInteresMora(operacion.getIdOperacion());

        FotoCarteraId fotoId = new FotoCarteraId(operacion.getIdOperacion(), fechaNegocio);
        Optional<FotoCartera> fotoExistente = this.fotoRepository.findById(fotoId);

        if (fotoExistente.isPresent()) {
            FotoCartera foto = fotoExistente.get();
            foto.setIdTramo(tramo.getIdTramo());
            foto.setEstadoCartera(operacion.getEstadoCartera());
            foto.setDiasMora(diasMora);
            foto.setSaldoCapital(saldoCapital);
            foto.setValorProvision(valorProvision);
            foto.setInteresDevengadoAcumulado(interesDevengado);
            foto.setInteresMoraAcumulado(interesMora);
            foto.setUsuarioModificacion("sistema");
            foto.setFechaModificacion(LocalDateTime.now());
            this.fotoRepository.save(foto);
        } else {
            FotoCartera foto = new FotoCartera();
            foto.setIdOperacion(operacion.getIdOperacion());
            foto.setFechaFoto(fechaNegocio);
            foto.setIdTramo(tramo.getIdTramo());
            foto.setEstadoCartera(operacion.getEstadoCartera());
            foto.setDiasMora(diasMora);
            foto.setSaldoCapital(saldoCapital);
            foto.setValorProvision(valorProvision);
            foto.setInteresDevengadoAcumulado(interesDevengado);
            foto.setInteresMoraAcumulado(interesMora);
            foto.setUsuarioCreacion("sistema");
            foto.setFechaCreacion(LocalDateTime.now());
            this.fotoRepository.save(foto);
        }
    }

    private BigDecimal calcularInteresDevengado(Integer idOperacion, LocalDate fechaNegocio) {
        BigDecimal desdeDevengos = this.devengoRepository.sumInteresDevengadoAcumulado(idOperacion, fechaNegocio);
        if (desdeDevengos != null && desdeDevengos.compareTo(BigDecimal.ZERO) > 0) {
            return desdeDevengos.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal desdeCuotas = this.cuotaRepository.sumInteresPendientePorOperacion(idOperacion);
        if (desdeCuotas != null && desdeCuotas.compareTo(BigDecimal.ZERO) > 0) {
            return desdeCuotas.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularInteresMora(Integer idOperacion) {
        BigDecimal mora = this.cuotaRepository.sumMoraPendientePorOperacion(idOperacion);
        if (mora != null && mora.compareTo(BigDecimal.ZERO) > 0) {
            return mora.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
