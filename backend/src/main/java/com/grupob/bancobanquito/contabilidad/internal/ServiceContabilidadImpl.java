package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.AsientoInvalidoException;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceContabilidadImpl implements ServiceContabilidad {

    private static final String USUARIO_AUDITORIA = "system";
    private static final int LONGITUD_MAXIMA_CLAVE = 60;

    private final AsientoContableRepository asientoRepository;
    private final CuentaContableRepository cuentaRepository;

    ServiceContabilidadImpl(AsientoContableRepository asientoRepository, CuentaContableRepository cuentaRepository) {
        this.asientoRepository = asientoRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    @Transactional
    public boolean registrarAsiento(MovimientoContable movimiento) {
        validar(movimiento);
        if (asientoRepository.existsByClaveIdempotencia(movimiento.claveIdempotencia())) {
            return false;
        }

        AsientoContable asiento = new AsientoContable();
        asiento.setIdOperacion(movimiento.idOperacion());
        asiento.setFechaAsiento(movimiento.fechaAsiento());
        asiento.setTipoAsiento(movimiento.tipoAsiento());
        asiento.setCuentaDebito(cuenta(movimiento.cuentaDebito()));
        asiento.setCuentaCredito(cuenta(movimiento.cuentaCredito()));
        asiento.setMontoAsiento(movimiento.monto());
        asiento.setClaveIdempotencia(movimiento.claveIdempotencia());
        asiento.setUsuarioCreacion(USUARIO_AUDITORIA);
        asiento.setFechaCreacion(LocalDateTime.now());
        asientoRepository.save(asiento);
        return true;
    }

    @Override
    @Transactional
    public int registrarAsientos(List<MovimientoContable> movimientos) {
        int registrados = 0;
        for (MovimientoContable movimiento : movimientos) {
            if (registrarAsiento(movimiento)) {
                registrados++;
            }
        }
        return registrados;
    }

    private void validar(MovimientoContable movimiento) {
        if (movimiento == null || movimiento.fechaAsiento() == null || movimiento.tipoAsiento() == null
                || movimiento.cuentaDebito() == null || movimiento.cuentaCredito() == null
                || movimiento.monto() == null) {
            throw new AsientoInvalidoException("El asiento contable tiene datos obligatorios vacíos.");
        }
        if (movimiento.cuentaDebito() == movimiento.cuentaCredito()) {
            throw new AsientoInvalidoException("La cuenta de débito y la de crédito deben ser distintas.");
        }
        if (movimiento.monto().compareTo(BigDecimal.ZERO) <= 0 || movimiento.monto().stripTrailingZeros().scale() > 2) {
            throw new AsientoInvalidoException(
                    "El monto del asiento debe ser mayor que cero y tener máximo dos decimales.");
        }
        if (movimiento.claveIdempotencia() == null || movimiento.claveIdempotencia().isBlank()
                || movimiento.claveIdempotencia().length() > LONGITUD_MAXIMA_CLAVE) {
            throw new AsientoInvalidoException(
                    "La clave de idempotencia es obligatoria y tiene máximo " + LONGITUD_MAXIMA_CLAVE + " caracteres.");
        }
    }

    private CuentaContable cuenta(CodigoCuentaContable codigo) {
        return cuentaRepository.findByCodigoCuenta(codigo)
                .orElseThrow(() -> new AsientoInvalidoException("La cuenta contable " + codigo + " no existe."));
    }
}
