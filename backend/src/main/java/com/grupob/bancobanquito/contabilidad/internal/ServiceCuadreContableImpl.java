package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.ResultadoCuadreContable;
import com.grupob.bancobanquito.contabilidad.ServiceCuadreContable;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ServiceCuadreContableImpl implements ServiceCuadreContable {

    private static final String ALCANCE_VERIFICACION =
            "Cada asiento registra el mismo monto en débito y crédito. "
                    + "Esta igualdad no verifica que todos los eventos de negocio estén contabilizados.";

    private final AsientoContableRepository asientoRepository;
    private final ServiceFechaNegocio servicioFechaNegocio;

    ServiceCuadreContableImpl(AsientoContableRepository asientoRepository,
                             ServiceFechaNegocio servicioFechaNegocio) {
        this.asientoRepository = asientoRepository;
        this.servicioFechaNegocio = servicioFechaNegocio;
    }

    @Override
    public ResultadoCuadreContable consultarCuadre(LocalDate fechaNegocio) {
        if (fechaNegocio == null) {
            throw new IllegalArgumentException("La fecha de negocio es obligatoria.");
        }
        List<AsientoContable> asientos = asientoRepository.findByFechaAsientoOrderByIdAsc(fechaNegocio);
        BigDecimal totalDebitos = BigDecimal.ZERO.setScale(2);
        BigDecimal totalCreditos = BigDecimal.ZERO.setScale(2);
        for (AsientoContable asiento : asientos) {
            totalDebitos = totalDebitos.add(asiento.getMontoAsiento());
            totalCreditos = totalCreditos.add(asiento.getMontoAsiento());
        }
        BigDecimal diferencia = totalDebitos.subtract(totalCreditos);
        String observacion = asientos.isEmpty()
                ? "No hay asientos registrados para la fecha consultada. " + ALCANCE_VERIFICACION
                : ALCANCE_VERIFICACION;
        return new ResultadoCuadreContable(fechaNegocio, totalDebitos, totalCreditos, diferencia,
                asientos.size(), diferencia.compareTo(BigDecimal.ZERO) == 0, observacion);
    }

    @Override
    public ResultadoCuadreContable consultarCuadreActual() {
        return consultarCuadre(servicioFechaNegocio.getCurrentBusinessDate());
    }
}
