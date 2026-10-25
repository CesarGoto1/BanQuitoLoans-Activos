package com.grupob.bancobanquito.amortization.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import com.grupob.bancobanquito.amortization.CalculatedInstallment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class AmortizationServiceTest {

    private static final BigDecimal AMOUNT = new BigDecimal("10000.00");
    private static final BigDecimal RATE = new BigDecimal("0.1677");
    private static final LocalDate DISBURSEMENT = LocalDate.of(2026, 10, 31);

    private final AmortizationServiceImpl amortizationService = new AmortizationServiceImpl();

    @Test
    void frenchScheduleMatchesTheReferenceExample() {
        List<CalculatedInstallment> installments = amortizationService.calculate(AMOUNT, RATE, 12,
                AmortizationMethod.FRENCH, DISBURSEMENT);

        String[][] expected = {
                {"2026-11-30", "775.41", "130.03", "9224.59"},
                {"2026-12-31", "785.49", "119.95", "8439.10"},
                {"2027-01-31", "795.70", "109.74", "7643.40"},
                {"2027-02-28", "806.05", "99.39", "6837.35"},
                {"2027-03-31", "816.53", "88.91", "6020.82"},
                {"2027-04-30", "827.15", "78.29", "5193.67"},
                {"2027-05-31", "837.90", "67.54", "4355.77"},
                {"2027-06-30", "848.80", "56.64", "3506.97"},
                {"2027-07-31", "859.84", "45.60", "2647.13"},
                {"2027-08-31", "871.02", "34.42", "1776.11"},
                {"2027-09-30", "882.34", "23.10", "893.77"},
                {"2027-10-31", "893.77", "11.62", "0.00"}};
        assertThat(installments).hasSize(12);
        for (int index = 0; index < expected.length; index++) {
            CalculatedInstallment installment = installments.get(index);
            assertThat(installment.installmentNumber()).isEqualTo(index + 1);
            assertThat(installment.dueDate()).isEqualTo(LocalDate.parse(expected[index][0]));
            assertThat(installment.principal()).isEqualByComparingTo(expected[index][1]);
            assertThat(installment.interest()).isEqualByComparingTo(expected[index][2]);
            assertThat(installment.remainingPrincipalBalance()).isEqualByComparingTo(expected[index][3]);
        }
        assertThat(totalInterest(installments)).isEqualByComparingTo("865.23");
    }

    @Test
    void germanScheduleMatchesTheReferenceExample() {
        List<CalculatedInstallment> installments = amortizationService.calculate(AMOUNT, RATE, 12,
                AmortizationMethod.GERMAN, DISBURSEMENT);

        assertThat(installments.get(0).principal()).isEqualByComparingTo("833.33");
        assertThat(installments.get(0).interest()).isEqualByComparingTo("130.03");
        assertThat(installments.get(1).interest()).isEqualByComparingTo("119.20");
        assertThat(installments.get(2).interest()).isEqualByComparingTo("108.36");
        assertThat(installments.get(11).principal()).isEqualByComparingTo("833.37");
        assertThat(installments.get(11).interest()).isEqualByComparingTo("10.84");
        assertThat(installments.get(11).remainingPrincipalBalance()).isEqualByComparingTo("0.00");
        assertThat(totalInterest(installments)).isEqualByComparingTo("845.23");
    }

    @Test
    void monthlyRateIsTheTwelfthRootOfTheEffectiveRate() {
        BigDecimal monthlyRate = amortizationService.monthlyRate(RATE);

        assertThat(BigDecimal.ONE.add(monthlyRate).pow(12).subtract(BigDecimal.ONE))
                .isCloseTo(RATE, within(new BigDecimal("1E-25")));
    }

    @ParameterizedTest
    @EnumSource(AmortizationMethod.class)
    void principalAddsUpToTheAmountForEveryAmountAndTerm(AmortizationMethod method) {
        for (String amount : new String[]{"500.00", "500.01", "12345.67", "30000.00"}) {
            for (int term : new int[]{6, 12, 37, 60}) {
                List<CalculatedInstallment> installments = amortizationService.calculate(new BigDecimal(amount),
                        new BigDecimal("0.1550"), term, method, DISBURSEMENT);

                assertThat(installments.stream().map(CalculatedInstallment::principal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(amount);
                assertThat(installments.getLast().remainingPrincipalBalance()).isZero();
                assertThat(installments).allSatisfy(installment -> {
                    assertThat(installment.principal()).isNotNegative();
                    assertThat(installment.remainingPrincipalBalance()).isNotNegative();
                });
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"2027-01-29,2027-02-28,2027-03-29", "2027-01-30,2027-02-28,2027-03-30",
            "2027-01-31,2027-02-28,2027-03-31", "2028-01-31,2028-02-29,2028-03-31"})
    void dueDatesKeepTheDisbursementDayAfterFebruary(LocalDate disbursement, LocalDate first, LocalDate second) {
        List<CalculatedInstallment> installments = amortizationService.calculate(new BigDecimal("500.00"),
                new BigDecimal("0.1550"), 6, AmortizationMethod.FRENCH, disbursement);

        assertThat(installments.get(0).dueDate()).isEqualTo(first);
        assertThat(installments.get(1).dueDate()).isEqualTo(second);
    }

    @Test
    void zeroRateDoesNotDivideByZero() {
        List<CalculatedInstallment> installments = amortizationService.calculate(new BigDecimal("1000.00"),
                BigDecimal.ZERO, 6, AmortizationMethod.FRENCH, DISBURSEMENT);

        assertThat(totalInterest(installments)).isZero();
        assertThat(installments.getLast().principal()).isEqualByComparingTo("166.65");
    }

    private BigDecimal totalInterest(List<CalculatedInstallment> installments) {
        return installments.stream().map(CalculatedInstallment::interest).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
