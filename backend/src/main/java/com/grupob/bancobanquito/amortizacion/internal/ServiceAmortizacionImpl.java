package com.grupob.bancobanquito.amortizacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.amortizacion.CuotaCalculada;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ServiceAmortizacionImpl implements ServiceAmortizacion {

    private static final MathContext PRECISION = new MathContext(34, RoundingMode.HALF_UP);
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal TOLERANCE = new BigDecimal("1E-30");
    private static final int MAX_ITERATIONS = 100;

    @Override
    public List<CuotaCalculada> calculate(BigDecimal monto, BigDecimal teaAsignada, Integer plazoMeses,
                                                 MetodoAmortizacion metodo, LocalDate fechaDesembolso) {
        BigDecimal monthlyRate = monthlyRate(teaAsignada);
        BigDecimal frenchPayment = frenchPayment(monto, monthlyRate, plazoMeses);
        BigDecimal germanPrincipal = monto.divide(BigDecimal.valueOf(plazoMeses), 2, RoundingMode.HALF_UP);

        List<CuotaCalculada> cuotas = new ArrayList<>();
        BigDecimal balance = monto;
        for (int number = 1; number <= plazoMeses; number++) {
            BigDecimal interes = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital;
            if (number == plazoMeses) {
                capital = balance;
            } else if (metodo == MetodoAmortizacion.FRANCES) {
                capital = frenchPayment.subtract(interes).min(balance);
            } else {
                capital = germanPrincipal.min(balance);
            }
            balance = balance.subtract(capital);
            cuotas.add(new CuotaCalculada(number, fechaDesembolso.plusMonths(number), capital, interes, balance));
        }
        return cuotas;
    }

    @Override
    public BigDecimal monthlyRate(BigDecimal teaAsignada) {
        BigDecimal annualFactor = BigDecimal.ONE.add(teaAsignada);
        BigDecimal root = BigDecimal.ONE;
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            BigDecimal next = root.multiply(BigDecimal.valueOf(11))
                    .add(annualFactor.divide(root.pow(11, PRECISION), PRECISION))
                    .divide(MONTHS_PER_YEAR, PRECISION);
            if (next.subtract(root).abs().compareTo(TOLERANCE) <= 0) {
                return next.subtract(BigDecimal.ONE);
            }
            root = next;
        }
        throw new IllegalStateException("No se pudo calcular la tasa mensual.");
    }

    private BigDecimal frenchPayment(BigDecimal monto, BigDecimal monthlyRate, Integer plazoMeses) {
        if (monthlyRate.signum() == 0) {
            return monto.divide(BigDecimal.valueOf(plazoMeses), 2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(plazoMeses, PRECISION);
        return monto.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }
}
