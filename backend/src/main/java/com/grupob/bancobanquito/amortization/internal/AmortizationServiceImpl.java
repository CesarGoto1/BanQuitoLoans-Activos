package com.grupob.bancobanquito.amortization.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import com.grupob.bancobanquito.amortization.AmortizationService;
import com.grupob.bancobanquito.amortization.CalculatedInstallment;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class AmortizationServiceImpl implements AmortizationService {

    private static final MathContext PRECISION = new MathContext(34, RoundingMode.HALF_UP);
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal TOLERANCE = new BigDecimal("1E-30");
    private static final int MAX_ITERATIONS = 100;

    @Override
    public List<CalculatedInstallment> calculate(BigDecimal amount, BigDecimal effectiveAnnualRate, Integer termMonths,
                                                 AmortizationMethod method, LocalDate disbursementDate) {
        BigDecimal monthlyRate = monthlyRate(effectiveAnnualRate);
        BigDecimal frenchPayment = frenchPayment(amount, monthlyRate, termMonths);
        BigDecimal germanPrincipal = amount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);

        List<CalculatedInstallment> installments = new ArrayList<>();
        BigDecimal balance = amount;
        for (int number = 1; number <= termMonths; number++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal;
            if (number == termMonths) {
                principal = balance;
            } else if (method == AmortizationMethod.FRENCH) {
                principal = frenchPayment.subtract(interest).min(balance);
            } else {
                principal = germanPrincipal.min(balance);
            }
            balance = balance.subtract(principal);
            installments.add(new CalculatedInstallment(number, disbursementDate.plusMonths(number), principal, interest, balance));
        }
        return installments;
    }

    @Override
    public BigDecimal monthlyRate(BigDecimal effectiveAnnualRate) {
        BigDecimal annualFactor = BigDecimal.ONE.add(effectiveAnnualRate);
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

    private BigDecimal frenchPayment(BigDecimal amount, BigDecimal monthlyRate, Integer termMonths) {
        if (monthlyRate.signum() == 0) {
            return amount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(termMonths, PRECISION);
        return amount.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }
}
