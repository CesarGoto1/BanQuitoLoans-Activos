package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ResumenFotoCartera;
import com.grupob.bancobanquito.cobranza.ResumenTramoCartera;
import com.grupob.bancobanquito.cobranza.ServiceFotografiaCartera;
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
public class ServiceFotografiaCarteraImpl implements ServiceFotografiaCartera {

    private final OperacionCobranzaRepository operacionRepository;
    private final CuotaCobranzaRepository cuotaRepository;
    private final TramoCarteraRepository tramoRepository;
    private final FotoCarteraRepository fotoRepository;
    private final DevengoCobranzaRepository devengoRepository;

    public ServiceFotografiaCarteraImpl(
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

    @Override
    @Transactional(readOnly = true)
    public List<ResumenFotoCartera> consultarFotosPorFecha(LocalDate fechaCorte) {
        return this.fotoRepository.findByFechaFoto(fechaCorte).stream()
                .map(this::mapearAFotoDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenFotoCartera> consultarFotosEntreFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        return this.fotoRepository.findByFechaFotoBetween(fechaInicio, fechaFin).stream()
                .map(this::mapearAFotoDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenTramoCartera> consultarTramos() {
        return this.tramoRepository.findAll().stream()
                .map(this::mapearATramoDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenTramoCartera> consultarTramosVigentes(LocalDate fechaCorte) {
        return this.tramoRepository.findTramosVigentes(fechaCorte).stream()
                .map(this::mapearATramoDto)
                .toList();
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

    private ResumenFotoCartera mapearAFotoDto(FotoCartera foto) {
        return new ResumenFotoCartera(
                foto.getIdOperacion(),
                foto.getFechaFoto(),
                foto.getIdTramo(),
                foto.getEstadoCartera(),
                foto.getDiasMora(),
                foto.getSaldoCapital(),
                foto.getValorProvision(),
                foto.getInteresDevengadoAcumulado(),
                foto.getInteresMoraAcumulado()
        );
    }

    private ResumenTramoCartera mapearATramoDto(TramoCartera tramo) {
        return new ResumenTramoCartera(
                tramo.getIdTramo(),
                tramo.getNumeroTramo(),
                tramo.getDiasMoraDesde(),
                tramo.getDiasMoraHasta(),
                tramo.getEstadoCartera(),
                tramo.getDevengaIngresos(),
                tramo.getTratamientoMora(),
                tramo.getPorcentajeProvision(),
                tramo.getFechaVigenciaDesde(),
                tramo.getFechaVigenciaHasta()
        );
    }
}
