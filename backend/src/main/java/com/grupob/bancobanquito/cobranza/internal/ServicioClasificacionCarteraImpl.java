package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ServicioClasificacionCartera;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class ServicioClasificacionCarteraImpl implements ServicioClasificacionCartera {

    private final OperacionCobranzaRepository operacionRepository;
    private final CuotaCobranzaRepository cuotaRepository;
    private final TramoCarteraRepository tramoRepository;
    private final CambioEstadoOperacionRepository cambioEstadoRepository;
    private final ServiceContabilidad serviceContabilidad;

    public ServicioClasificacionCarteraImpl(
            OperacionCobranzaRepository operacionRepository,
            CuotaCobranzaRepository cuotaRepository,
            TramoCarteraRepository tramoRepository,
            CambioEstadoOperacionRepository cambioEstadoRepository,
            ServiceContabilidad serviceContabilidad) {
        this.operacionRepository = operacionRepository;
        this.cuotaRepository = cuotaRepository;
        this.tramoRepository = tramoRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.serviceContabilidad = serviceContabilidad;
    }

    @Override
    public int clasificarCartera(LocalDate fechaNegocio) {
        List<OperacionCobranza> operaciones = this.operacionRepository.findOperacionesActivas();
        int procesadas = 0;

        for (OperacionCobranza operacion : operaciones) {
            procesarOperacion(operacion, fechaNegocio);
            procesadas++;
        }

        return procesadas;
    }

    private void procesarOperacion(OperacionCobranza operacion, LocalDate fechaNegocio) {
        int diasMora = calcularDiasMora(operacion.getIdOperacion(), fechaNegocio);
        operacion.setDiasMora(diasMora);

        String estadoActual = operacion.getEstadoCartera();
        if ("CASTIGADA".equalsIgnoreCase(estadoActual)) {
            this.operacionRepository.save(operacion);
            return;
        }

        TramoCartera tramo = this.tramoRepository.findTramoPorDiasMora(diasMora, fechaNegocio)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro un tramo de cartera vigente para " + diasMora + " dias de mora en " + fechaNegocio));

        String estadoSugerido = tramo.getEstadoCartera();

        if (!estadoSugerido.equalsIgnoreCase(estadoActual)) {
            operacion.setEstadoCartera(estadoSugerido);
            operacion.setUsuarioModificacion("sistema");
            operacion.setFechaModificacion(LocalDateTime.now());
            this.operacionRepository.save(operacion);

            registrarCambioEstado(operacion.getIdOperacion(), fechaNegocio, estadoActual, estadoSugerido, diasMora);
            generarAsientoContable(operacion, fechaNegocio, estadoActual, estadoSugerido);
        } else {
            this.operacionRepository.save(operacion);
        }
    }

    private int calcularDiasMora(Integer idOperacion, LocalDate fechaNegocio) {
        List<CuotaCobranza> cuotasVencidas = this.cuotaRepository.findCuotasVencidasPendientes(idOperacion, fechaNegocio);
        if (cuotasVencidas.isEmpty()) {
            return 0;
        }
        LocalDate fechaMasAntigua = cuotasVencidas.get(0).getFechaVencimiento();
        long dias = ChronoUnit.DAYS.between(fechaMasAntigua, fechaNegocio);
        return (int) Math.max(0, dias);
    }

    private void registrarCambioEstado(Integer idOperacion, LocalDate fechaNegocio,
                                       String estadoAnterior, String estadoNuevo, int diasMora) {
        if (!this.cambioEstadoRepository.existsByIdOperacionAndFechaCambio(idOperacion, fechaNegocio)) {
            CambioEstadoOperacion cambio = new CambioEstadoOperacion();
            cambio.setIdOperacion(idOperacion);
            cambio.setFechaCambio(fechaNegocio);
            cambio.setEstadoAnterior(estadoAnterior);
            cambio.setEstadoNuevo(estadoNuevo);
            cambio.setDiasMora(diasMora);
            cambio.setUsuarioCreacion("sistema");
            cambio.setFechaCreacion(LocalDateTime.now());
            this.cambioEstadoRepository.save(cambio);
        }
    }

    private void generarAsientoContable(OperacionCobranza operacion, LocalDate fechaNegocio,
                                        String estadoAnterior, String estadoNuevo) {
        if (operacion.getSaldoCapital() == null || operacion.getSaldoCapital().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal montoAsiento = operacion.getSaldoCapital().setScale(2, RoundingMode.HALF_UP);

        if ("CASTIGADA".equalsIgnoreCase(estadoNuevo)) {
            String clave = "CASTIGO-ORDEN-" + operacion.getIdOperacion() + "-" + fechaNegocio;
            MovimientoContable movimiento = new MovimientoContable(
                    operacion.getIdOperacion(),
                    fechaNegocio,
                    TipoAsiento.CASTIGO,
                    CodigoCuentaContable.CARTERA_CASTIGADA,
                    CodigoCuentaContable.CONTRA_CUENTAS_ORDEN,
                    montoAsiento,
                    clave
            );
            this.serviceContabilidad.registrarAsiento(movimiento);
        } else {
            CodigoCuentaContable cuentaDebito = mapearCuentaCartera(estadoNuevo);
            CodigoCuentaContable cuentaCredito = mapearCuentaCartera(estadoAnterior);
            String clave = "RECLASIF-" + operacion.getIdOperacion() + "-" + fechaNegocio + "-" + estadoNuevo;

            MovimientoContable movimiento = new MovimientoContable(
                    operacion.getIdOperacion(),
                    fechaNegocio,
                    TipoAsiento.RECLASIFICACION,
                    cuentaDebito,
                    cuentaCredito,
                    montoAsiento,
                    clave
            );
            this.serviceContabilidad.registrarAsiento(movimiento);
        }
    }

    private CodigoCuentaContable mapearCuentaCartera(String estado) {
        return switch (estado.toUpperCase()) {
            case "POR_VENCER" -> CodigoCuentaContable.CARTERA_POR_VENCER;
            case "VENCIDA" -> CodigoCuentaContable.CARTERA_VENCIDA;
            case "EN_LITIGIO" -> CodigoCuentaContable.CARTERA_EN_LITIGIO;
            case "CASTIGADA" -> CodigoCuentaContable.CARTERA_CASTIGADA;
            default -> throw new IllegalArgumentException("Estado de cartera no reconocido: " + estado);
        };
    }
}
