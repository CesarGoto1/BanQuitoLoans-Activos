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
class SimulatedAmortizationService implements AmortizationService {

    @Override
    public List<CalculatedInstallment> calculate(BigDecimal amount, BigDecimal effectiveAnnualRate, Integer termMonths,
                                                 AmortizationMethod method, LocalDate disbursementDate) {
        double monthly = Math.pow(1 + effectiveAnnualRate.doubleValue(), 1.0 / 12) - 1;
        BigDecimal monthlyRate = new BigDecimal(monthly, MathContext.DECIMAL64);
        BigDecimal frenchPayment = amount.multiply(monthlyRate)
                .divide(BigDecimal.ONE.subtract(BigDecimal.valueOf(Math.pow(1 + monthly, -termMonths))), 2, RoundingMode.HALF_UP);
        BigDecimal germanPrincipal = amount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);

        List<CalculatedInstallment> installments = new ArrayList<>();
        BigDecimal balance = amount;
        for (int number = 1; number <= termMonths; number++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal;
            if (number == termMonths) {
                principal = balance;
            } else if (method == AmortizationMethod.FRENCH) {
                principal = frenchPayment.subtract(interest);
            } else {
                principal = germanPrincipal;
            }
            balance = balance.subtract(principal);
            installments.add(new CalculatedInstallment(number, disbursementDate.plusMonths(number), principal, interest, balance));
        }
        return installments;
    }
}
