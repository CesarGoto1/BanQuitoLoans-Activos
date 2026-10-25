package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.amortization.AmortizacionService;
import com.grupob.bancobanquito.amortization.CondicionesAmortizacion;
import com.grupob.bancobanquito.parameters.BusinessDateService;
import com.grupob.bancobanquito.parameters.ParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SimulacionServiceImpl implements SimulacionService {

    private final ParameterService parametroService;
    private final BusinessDateService fechaNegocioService;
    private final AmortizacionService amortizacionService;

    SimulacionServiceImpl(ParameterService parametroService, BusinessDateService fechaNegocioService,
            AmortizacionService amortizacionService) {
        this.parametroService = parametroService;
        this.fechaNegocioService = fechaNegocioService;
        this.amortizacionService = amortizacionService;
    }

    @Override
    public ParametrosSimulacionResponse consultarParametros() {
        LocalDate fecha = fechaNegocioService.getCurrentBusinessDate();
        var parametros = new ParametrosSimulacionResponse(fecha,
                parametroService.getValue("PAR-01", fecha), parametroService.getValue("PAR-02", fecha),
                parametroService.getValue("PAR-03", fecha).intValueExact(),
                parametroService.getValue("PAR-04", fecha).intValueExact(),
                parametroService.getValue("PAR-06", fecha), parametroService.getValue("PAR-07", fecha),
                parametroService.getValue("PAR-08", fecha));
        BigDecimal teaMaxima = parametroService.getValue("PAR-05", fecha);
        if (parametros.montoMinimo().signum() <= 0 || parametros.montoMaximo().compareTo(parametros.montoMinimo()) < 0
                || parametros.plazoMinimo() <= 0 || parametros.plazoMaximo() < parametros.plazoMinimo()
                || parametros.teaReferencial().signum() <= 0 || parametros.teaReferencial().compareTo(teaMaxima) > 0
                || parametros.factorSeguroMensual().signum() < 0 || parametros.factorSolca().signum() < 0
                || parametros.factorSolca().compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalStateException("Los parámetros de simulación no son coherentes.");
        }
        return parametros;
    }

    @Override
    public SimulacionResponse simular(SimulacionRequest solicitud) {
        var parametros = consultarParametros();
        if (solicitud.montoSolicitado().compareTo(parametros.montoMinimo()) < 0
                || solicitud.montoSolicitado().compareTo(parametros.montoMaximo()) > 0) {
            throw new CondicionesSimulacionException("El monto debe estar entre USD "
                    + parametros.montoMinimo().setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + " y USD " + parametros.montoMaximo().setScale(2, RoundingMode.HALF_UP).toPlainString() + ".");
        }
        if (solicitud.plazoMeses() < parametros.plazoMinimo() || solicitud.plazoMeses() > parametros.plazoMaximo()) {
            throw new CondicionesSimulacionException("El plazo debe estar entre " + parametros.plazoMinimo()
                    + " y " + parametros.plazoMaximo() + " meses.");
        }
        BigDecimal monto = solicitud.montoSolicitado().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal seguro = monto.multiply(parametros.factorSeguroMensual()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal solca = monto.multiply(parametros.factorSolca()).setScale(2, RoundingMode.HALF_UP);
        var tabla = amortizacionService.generarTabla(new CondicionesAmortizacion(monto,
                solicitud.plazoMeses(), parametros.teaReferencial(), seguro, parametros.fechaNegocio(),
                solicitud.sistemaAmortizacion()));
        return new SimulacionResponse(parametros.fechaNegocio(), monto, solicitud.plazoMeses(),
                solicitud.sistemaAmortizacion(), parametros.teaReferencial(), seguro, solca, monto.subtract(solca),
                tabla.cuotas().getFirst().totalCuota(), tabla.cuotas().getLast().totalCuota(),
                tabla.totalIntereses().add(tabla.totalSeguros()).add(solca), tabla);
    }
}
