package com.grupob.bancobanquito.amortizacion.internal;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.amortizacion.ModalidadAbono;
import com.grupob.bancobanquito.amortizacion.RecalculoTabla;
import com.grupob.bancobanquito.amortizacion.ServiceAmortizacion;
import com.grupob.bancobanquito.amortizacion.CuotaCalculada;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    @Override
    public BigDecimal interesPorDias(BigDecimal saldoCapital, BigDecimal teaAsignada, long dias, Integer baseDias) {
        return saldoCapital.multiply(tasaPorDias(teaAsignada, dias, baseDias)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<CuotaCalculada> recalcular(RecalculoTabla recalculo) {
        BigDecimal monthlyRate = monthlyRate(recalculo.teaAsignada());
        List<LocalDate> vencimientos = recalculo.vencimientosPendientes();
        long diasPrimerPeriodo = ChronoUnit.DAYS.between(recalculo.fechaAbono(), vencimientos.getFirst());
        BigDecimal tasaPrimerPeriodo = tasaPorDias(recalculo.teaAsignada(), diasPrimerPeriodo, recalculo.baseDias());
        BigDecimal valorFijo = valorFijo(recalculo, monthlyRate, tasaPrimerPeriodo);

        List<CuotaCalculada> cuotas = new ArrayList<>();
        BigDecimal balance = recalculo.saldoCapital();
        for (int index = 0; index < vencimientos.size() && balance.signum() > 0; index++) {
            BigDecimal tasa = index == 0 ? tasaPrimerPeriodo : monthlyRate;
            BigDecimal interes = balance.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital = recalculo.metodo() == MetodoAmortizacion.FRANCES
                    ? valorFijo.subtract(interes)
                    : valorFijo;
            if (index == vencimientos.size() - 1 || capital.compareTo(balance) > 0) {
                capital = balance;
            }
            balance = balance.subtract(capital);
            cuotas.add(new CuotaCalculada(recalculo.primeraCuota() + index, vencimientos.get(index),
                    capital, interes, balance));
        }
        return cuotas;
    }

    private BigDecimal valorFijo(RecalculoTabla recalculo, BigDecimal monthlyRate, BigDecimal tasaPrimerPeriodo) {
        if (recalculo.modalidad() == ModalidadAbono.DISMINUIR_PLAZO) {
            return recalculo.cuotaVigente();
        }
        int cuotasRestantes = recalculo.vencimientosPendientes().size();
        if (recalculo.metodo() == MetodoAmortizacion.ALEMAN || monthlyRate.signum() == 0) {
            return recalculo.saldoCapital().divide(BigDecimal.valueOf(cuotasRestantes), 2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(cuotasRestantes, PRECISION);
        BigDecimal factorAnterior = BigDecimal.ONE.add(monthlyRate).pow(cuotasRestantes - 1, PRECISION);
        return recalculo.saldoCapital()
                .multiply(BigDecimal.ONE.add(tasaPrimerPeriodo))
                .multiply(monthlyRate)
                .multiply(factorAnterior)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal tasaPorDias(BigDecimal teaAsignada, long dias, Integer baseDias) {
        return monthlyRate(teaAsignada).multiply(MONTHS_PER_YEAR)
                .multiply(BigDecimal.valueOf(dias))
                .divide(BigDecimal.valueOf(baseDias), PRECISION);
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
