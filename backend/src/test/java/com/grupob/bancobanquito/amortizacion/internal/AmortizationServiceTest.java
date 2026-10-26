package com.grupob.bancobanquito.amortizacion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.grupob.bancobanquito.amortizacion.MetodoAmortizacion;
import com.grupob.bancobanquito.amortizacion.CuotaCalculada;
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

    private final ServiceAmortizacionImpl servicioAmortizacion = new ServiceAmortizacionImpl();

    @Test
    void frenchScheduleMatchesTheReferenceExample() {
        List<CuotaCalculada> cuotas = servicioAmortizacion.calculate(AMOUNT, RATE, 12,
                MetodoAmortizacion.FRENCH, DISBURSEMENT);

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
        assertThat(cuotas).hasSize(12);
        for (int index = 0; index < expected.length; index++) {
            CuotaCalculada installment = cuotas.get(index);
            assertThat(installment.numeroCuota()).isEqualTo(index + 1);
            assertThat(installment.fechaVencimiento()).isEqualTo(LocalDate.parse(expected[index][0]));
            assertThat(installment.capital()).isEqualByComparingTo(expected[index][1]);
            assertThat(installment.interes()).isEqualByComparingTo(expected[index][2]);
            assertThat(installment.remainingPrincipalBalance()).isEqualByComparingTo(expected[index][3]);
        }
        assertThat(interesTotal(cuotas)).isEqualByComparingTo("865.23");
    }

    @Test
    void germanScheduleMatchesTheReferenceExample() {
        List<CuotaCalculada> cuotas = servicioAmortizacion.calculate(AMOUNT, RATE, 12,
                MetodoAmortizacion.GERMAN, DISBURSEMENT);

        assertThat(cuotas.get(0).capital()).isEqualByComparingTo("833.33");
        assertThat(cuotas.get(0).interes()).isEqualByComparingTo("130.03");
        assertThat(cuotas.get(1).interes()).isEqualByComparingTo("119.20");
        assertThat(cuotas.get(2).interes()).isEqualByComparingTo("108.36");
        assertThat(cuotas.get(11).capital()).isEqualByComparingTo("833.37");
        assertThat(cuotas.get(11).interes()).isEqualByComparingTo("10.84");
        assertThat(cuotas.get(11).remainingPrincipalBalance()).isEqualByComparingTo("0.00");
        assertThat(interesTotal(cuotas)).isEqualByComparingTo("845.23");
    }

    @Test
    void monthlyRateIsTheTwelfthRootOfTheEffectiveRate() {
        BigDecimal monthlyRate = servicioAmortizacion.monthlyRate(RATE);

        assertThat(BigDecimal.ONE.add(monthlyRate).pow(12).subtract(BigDecimal.ONE))
                .isCloseTo(RATE, within(new BigDecimal("1E-25")));
    }

    @ParameterizedTest
    @EnumSource(MetodoAmortizacion.class)
    void principalAddsUpToTheAmountForEveryAmountAndTerm(MetodoAmortizacion metodo) {
        for (String monto : new String[]{"500.00", "500.01", "12345.67", "30000.00"}) {
            for (int term : new int[]{6, 12, 37, 60}) {
                List<CuotaCalculada> cuotas = servicioAmortizacion.calculate(new BigDecimal(monto),
                        new BigDecimal("0.1550"), term, metodo, DISBURSEMENT);

                assertThat(cuotas.stream().map(CuotaCalculada::capital)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(monto);
                assertThat(cuotas.getLast().remainingPrincipalBalance()).isZero();
                assertThat(cuotas).allSatisfy(installment -> {
                    assertThat(installment.capital()).isNotNegative();
                    assertThat(installment.remainingPrincipalBalance()).isNotNegative();
                });
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"2027-01-29,2027-02-28,2027-03-29", "2027-01-30,2027-02-28,2027-03-30",
            "2027-01-31,2027-02-28,2027-03-31", "2028-01-31,2028-02-29,2028-03-31"})
    void dueDatesKeepTheDisbursementDayAfterFebruary(LocalDate disbursement, LocalDate first, LocalDate second) {
        List<CuotaCalculada> cuotas = servicioAmortizacion.calculate(new BigDecimal("500.00"),
                new BigDecimal("0.1550"), 6, MetodoAmortizacion.FRENCH, disbursement);

        assertThat(cuotas.get(0).fechaVencimiento()).isEqualTo(first);
        assertThat(cuotas.get(1).fechaVencimiento()).isEqualTo(second);
    }

    @Test
    void zeroRateDoesNotDivideByZero() {
        List<CuotaCalculada> cuotas = servicioAmortizacion.calculate(new BigDecimal("1000.00"),
                BigDecimal.ZERO, 6, MetodoAmortizacion.FRENCH, DISBURSEMENT);

        assertThat(interesTotal(cuotas)).isZero();
        assertThat(cuotas.getLast().capital()).isEqualByComparingTo("166.65");
    }

    private BigDecimal interesTotal(List<CuotaCalculada> cuotas) {
        return cuotas.stream().map(CuotaCalculada::interes).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
