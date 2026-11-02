package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.AsientoInvalidoException;
import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import com.grupob.bancobanquito.contabilidad.DatosProvision;
import com.grupob.bancobanquito.contabilidad.MovimientoContable;
import com.grupob.bancobanquito.contabilidad.ResultadoProvisiones;
import com.grupob.bancobanquito.contabilidad.ServiceContabilidad;
import com.grupob.bancobanquito.contabilidad.ServiceProvisiones;
import com.grupob.bancobanquito.contabilidad.TipoAsiento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ServiceProvisionesImpl implements ServiceProvisiones {

    private static final BigDecimal UNO = BigDecimal.ONE;
    private static final int ESCALA_MONETARIA = 2;
    private static final int ESCALA_PORCENTAJE = 4;

    private final ServiceContabilidad servicioContabilidad;

    ServiceProvisionesImpl(ServiceContabilidad servicioContabilidad) {
        this.servicioContabilidad = servicioContabilidad;
    }

    @Override
    @Transactional
    public ResultadoProvisiones registrarProvisiones(LocalDate fechaAsiento, List<DatosProvision> operaciones) {
        if (fechaAsiento == null) {
            throw new AsientoInvalidoException("La fecha de provisiones es obligatoria.");
        }
        if (operaciones == null) {
            throw new AsientoInvalidoException("La lista de operaciones para provisiones es obligatoria.");
        }

        List<MovimientoContable> movimientos = new ArrayList<>();
        BigDecimal provisionTotal = BigDecimal.ZERO.setScale(ESCALA_MONETARIA);
        BigDecimal variacionTotal = BigDecimal.ZERO.setScale(ESCALA_MONETARIA);

        for (DatosProvision operacion : operaciones) {
            validar(operacion);
            BigDecimal provisionActual = operacion.saldoCapital()
                    .multiply(operacion.porcentajeProvision())
                    .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            BigDecimal variacion = provisionActual.subtract(operacion.provisionAnterior())
                    .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            provisionTotal = provisionTotal.add(provisionActual);
            variacionTotal = variacionTotal.add(variacion);
            if (variacion.signum() != 0) {
                movimientos.add(movimiento(fechaAsiento, operacion, variacion));
            }
        }

        int asientosRegistrados = servicioContabilidad.registrarAsientos(movimientos);
        return new ResultadoProvisiones(fechaAsiento, operaciones.size(), asientosRegistrados,
                provisionTotal, variacionTotal);
    }

    private MovimientoContable movimiento(LocalDate fechaAsiento, DatosProvision operacion, BigDecimal variacion) {
        boolean aumento = variacion.signum() > 0;
        CodigoCuentaContable debito = aumento
                ? CodigoCuentaContable.GASTO_PROVISIONES
                : CodigoCuentaContable.PROVISIONES_CARTERA;
        CodigoCuentaContable credito = aumento
                ? CodigoCuentaContable.PROVISIONES_CARTERA
                : CodigoCuentaContable.GASTO_PROVISIONES;
        String clave = "PROVISION-" + fechaAsiento + "-" + operacion.idOperacion();
        return new MovimientoContable(operacion.idOperacion(), fechaAsiento, TipoAsiento.PROVISION,
                debito, credito, variacion.abs(), clave);
    }

    private void validar(DatosProvision operacion) {
        if (operacion == null || operacion.idOperacion() == null || operacion.saldoCapital() == null
                || operacion.porcentajeProvision() == null || operacion.provisionAnterior() == null
                || operacion.estadoCartera() == null) {
            throw new AsientoInvalidoException("Los datos de provisión tienen campos obligatorios vacíos.");
        }
        if (operacion.idOperacion() <= 0) {
            throw new AsientoInvalidoException("El identificador de operación debe ser positivo.");
        }
        validarMontoNoNegativo(operacion.saldoCapital(), "El saldo de capital");
        validarMontoNoNegativo(operacion.provisionAnterior(), "La provisión anterior");
        if (operacion.porcentajeProvision().compareTo(BigDecimal.ZERO) < 0
                || operacion.porcentajeProvision().compareTo(UNO) > 0
                || operacion.porcentajeProvision().stripTrailingZeros().scale() > ESCALA_PORCENTAJE) {
            throw new AsientoInvalidoException("El porcentaje de provisión debe estar entre cero y uno.");
        }
        if (!estadoValido(operacion.estadoCartera())) {
            throw new AsientoInvalidoException("El estado de cartera no es válido para una provisión.");
        }
    }

    private boolean estadoValido(String estadoCartera) {
        return switch (estadoCartera.toUpperCase()) {
            case "POR_VENCER", "VENCIDA", "EN_LITIGIO", "CASTIGADA" -> true;
            default -> false;
        };
    }

    private void validarMontoNoNegativo(BigDecimal monto, String nombre) {
        if (monto.signum() < 0 || monto.stripTrailingZeros().scale() > ESCALA_MONETARIA) {
            throw new AsientoInvalidoException(nombre + " debe ser no negativo y tener máximo dos decimales.");
        }
    }
}
