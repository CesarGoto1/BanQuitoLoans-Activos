package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ResultadoProvisiones;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.ServiceProvisiones;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import com.grupob.bancobanquito.prestamos.EstadoCartera;
import com.grupob.bancobanquito.prestamos.internal.Prestamo;
import com.grupob.bancobanquito.prestamos.internal.PrestamoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceProvisionesImpl implements ServiceProvisiones {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final PrestamoRepository prestamoRepository;
    private final TramoCarteraRepository tramoRepository;
    private final FotoCarteraRepository fotoRepository;
    private final ServiceContabilidad contabilidad;

    ServiceProvisionesImpl(PrestamoRepository prestamoRepository,
                           TramoCarteraRepository tramoRepository,
                           FotoCarteraRepository fotoRepository,
                           ServiceContabilidad contabilidad) {
        this.prestamoRepository = prestamoRepository;
        this.tramoRepository = tramoRepository;
        this.fotoRepository = fotoRepository;
        this.contabilidad = contabilidad;
    }

    @Override
    @Transactional
    public ResultadoProvisiones calcular(LocalDate fechaNegocio) {
        List<TramoCartera> tramos = tramoRepository.findAll();
        int procesadas = 0;
        int asientos = 0;
        BigDecimal variacionTotal = ZERO;

        for (Prestamo prestamo : prestamoRepository.findByPortfolioStatusNot(EstadoCartera.CANCELADA)) {
            TramoCartera tramo = tramos.stream()
                    .filter(candidate -> candidate.appliesTo(prestamo.getDaysPastDue(), fechaNegocio))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe tramo vigente para la operación " + prestamo.getLoanNumber()));
            if (fotoRepository.findById(new FotoCarteraId(prestamo.getId(), fechaNegocio)).isPresent()) {
                procesadas++;
                continue;
            }

            List<FotoCartera> anteriores = fotoRepository.findPrevious(prestamo.getId(), fechaNegocio);
            FotoCartera anterior = anteriores.stream().findFirst().orElse(null);
            BigDecimal provisionActual = prestamo.getPrincipalBalance()
                    .multiply(tramo.getProvisionRate())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal provisionAnterior = anterior == null ? ZERO : anterior.getProvision();
            BigDecimal variacion = provisionActual.subtract(provisionAnterior)
                    .setScale(2, RoundingMode.HALF_UP);

            if (variacion.signum() != 0) {
                CodigoCuentaContable debito = variacion.signum() > 0
                        ? CodigoCuentaContable.GASTO_PROVISIONES
                        : CodigoCuentaContable.PROVISIONES_CARTERA;
                CodigoCuentaContable credito = variacion.signum() > 0
                        ? CodigoCuentaContable.PROVISIONES_CARTERA
                        : CodigoCuentaContable.GASTO_PROVISIONES;
                asientos += registrar(new MovimientoContable(prestamo.getId(), fechaNegocio,
                        TipoAsiento.PROVISION, debito, credito, variacion.abs(),
                        "PROVISION:" + fechaNegocio + ":" + prestamo.getId()));
            }

            if (prestamo.getPortfolioStatus() == EstadoCartera.CASTIGADA
                    && (anterior == null || !"CASTIGADA".equals(anterior.getStatus()))) {
                if (provisionAnterior.signum() > 0) {
                    asientos += registrar(new MovimientoContable(prestamo.getId(), fechaNegocio,
                            TipoAsiento.CASTIGO, CodigoCuentaContable.PROVISIONES_CARTERA,
                            CodigoCuentaContable.CARTERA_EN_LITIGIO, provisionAnterior,
                            "CASTIGO:" + fechaNegocio + ":" + prestamo.getId()));
                }
                asientos += registrar(new MovimientoContable(prestamo.getId(), fechaNegocio,
                        TipoAsiento.CASTIGO, CodigoCuentaContable.CARTERA_CASTIGADA,
                        CodigoCuentaContable.CONTRA_CUENTAS_ORDEN, prestamo.getPrincipalBalance(),
                        "CASTIGO_ORDEN:" + fechaNegocio + ":" + prestamo.getId()));
            }

            fotoRepository.save(FotoCartera.create(prestamo, fechaNegocio, tramo, provisionActual));
            variacionTotal = variacionTotal.add(variacion);
            procesadas++;
        }
        return new ResultadoProvisiones(fechaNegocio, procesadas, asientos,
                variacionTotal.setScale(2, RoundingMode.HALF_UP));
    }

    private int registrar(MovimientoContable movimiento) {
        return contabilidad.registrarAsiento(movimiento) ? 1 : 0;
    }
}
