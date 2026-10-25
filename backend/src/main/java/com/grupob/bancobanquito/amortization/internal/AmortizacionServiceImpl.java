package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.amortization.AmortizacionService;
import com.grupob.bancobanquito.amortization.CondicionesAmortizacion;
import com.grupob.bancobanquito.amortization.CuotaAmortizacion;
import com.grupob.bancobanquito.amortization.TablaAmortizacion;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class AmortizacionServiceImpl implements AmortizacionService {

    private static final MathContext PRECISION = new MathContext(34, RoundingMode.HALF_UP);
    private static final BigDecimal MESES_ANIO = BigDecimal.valueOf(12);
    private static final BigDecimal TOLERANCIA = new BigDecimal("1E-30");

    @Override
    public TablaAmortizacion generarTabla(CondicionesAmortizacion condiciones) {
        validar(condiciones);
        BigDecimal saldo = dinero(condiciones.montoAprobado());
        BigDecimal seguro = dinero(condiciones.seguroMensual());
        BigDecimal tasaMensual = calcularTasaMensual(condiciones.teaAsignada());
        BigDecimal cuotaFrancesa = calcularCuotaFrancesa(saldo, tasaMensual, condiciones.plazoMeses());
        BigDecimal capitalAleman = saldo.divide(BigDecimal.valueOf(condiciones.plazoMeses()), 2, RoundingMode.HALF_UP);
        BigDecimal totalIntereses = dinero(BigDecimal.ZERO);
        var cuotas = new ArrayList<CuotaAmortizacion>();

        for (int numero = 1; numero <= condiciones.plazoMeses(); numero++) {
            BigDecimal interes = dinero(saldo.multiply(tasaMensual));
            BigDecimal capital = switch (condiciones.sistemaAmortizacion()) {
                case FRANCES -> cuotaFrancesa.subtract(interes);
                case ALEMAN -> capitalAleman;
            };
            capital = numero == condiciones.plazoMeses() ? saldo : capital.min(saldo);
            saldo = saldo.subtract(capital);
            cuotas.add(new CuotaAmortizacion(numero, condiciones.fechaDesembolso().plusMonths(numero),
                    capital, interes, seguro, capital.add(interes).add(seguro), saldo));
            totalIntereses = totalIntereses.add(interes);
        }

        BigDecimal totalCapital = dinero(condiciones.montoAprobado());
        BigDecimal totalSeguros = seguro.multiply(BigDecimal.valueOf(condiciones.plazoMeses()));
        return new TablaAmortizacion(tasaMensual, tasaMensual.multiply(MESES_ANIO),
                totalCapital, totalIntereses, totalSeguros,
                totalCapital.add(totalIntereses).add(totalSeguros), cuotas);
    }

    private BigDecimal calcularTasaMensual(BigDecimal tea) {
        BigDecimal factorAnual = BigDecimal.ONE.add(tea);
        BigDecimal raiz = BigDecimal.ONE;
        for (int intento = 0; intento < 100; intento++) {
            BigDecimal siguiente = raiz.multiply(BigDecimal.valueOf(11))
                    .add(factorAnual.divide(raiz.pow(11, PRECISION), PRECISION))
                    .divide(MESES_ANIO, PRECISION);
            if (siguiente.subtract(raiz).abs().compareTo(TOLERANCIA) <= 0) {
                return siguiente.subtract(BigDecimal.ONE);
            }
            raiz = siguiente;
        }
        throw new IllegalArgumentException("No se pudo obtener la tasa mensual de las condiciones indicadas.");
    }

    private BigDecimal calcularCuotaFrancesa(BigDecimal monto, BigDecimal tasa, int plazo) {
        if (tasa.signum() == 0) {
            return monto.divide(BigDecimal.valueOf(plazo), 2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(tasa).pow(plazo, PRECISION);
        return monto.multiply(tasa).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private void validar(CondicionesAmortizacion condiciones) {
        Objects.requireNonNull(condiciones, "Las condiciones son obligatorias.");
        Objects.requireNonNull(condiciones.montoAprobado(), "El monto es obligatorio.");
        Objects.requireNonNull(condiciones.teaAsignada(), "La TEA es obligatoria.");
        Objects.requireNonNull(condiciones.seguroMensual(), "El seguro es obligatorio.");
        Objects.requireNonNull(condiciones.fechaDesembolso(), "La fecha es obligatoria.");
        Objects.requireNonNull(condiciones.sistemaAmortizacion(), "El sistema es obligatorio.");
        if (condiciones.montoAprobado().signum() <= 0 || condiciones.plazoMeses() <= 0
                || condiciones.teaAsignada().signum() < 0 || condiciones.seguroMensual().signum() < 0) {
            throw new IllegalArgumentException("El monto y el plazo deben ser positivos; la tasa y el seguro no pueden ser negativos.");
        }
        condiciones.montoAprobado().setScale(2, RoundingMode.UNNECESSARY);
        condiciones.seguroMensual().setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
