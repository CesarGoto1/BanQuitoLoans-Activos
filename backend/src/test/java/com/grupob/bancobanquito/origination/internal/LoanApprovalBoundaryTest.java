package com.grupob.bancobanquito.origination.internal;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class LoanApprovalBoundaryTest {

    private final LoanApprovalPolicy policy = new LoanApprovalPolicy();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01", "0"})
    void refusesMissingOrNonPositiveRate(String rate) {
        assertThatThrownBy(() -> policy.validateAnnualEffectiveRate(rate == null ? null : new BigDecimal(rate),
                new BigDecimal("0.1677"))).isInstanceOf(LoanDecisionRuleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void refusesNonPositiveSurplus(String surplus) {
        assertThatThrownBy(() -> policy.validatePaymentCapacity(new BigDecimal("1000").add(new BigDecimal(surplus)),
                new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("0.4")))
                .isInstanceOf(LoanDecisionRuleException.class);
    }

    @Test
    void acceptsInstallmentExactlyAtMonetaryLimit() {
        assertThat(policy.validatePaymentCapacity(new BigDecimal("1500"), new BigDecimal("500"),
                new BigDecimal("400.00"), new BigDecimal("0.4"))).isEqualByComparingTo("400.00");
    }

    @Test
    void refusesOneCentAboveLimitEvenWhenPercentageRoundsToForty() {
        assertThatThrownBy(() -> policy.validatePaymentCapacity(new BigDecimal("1500"), new BigDecimal("500"),
                new BigDecimal("400.01"), new BigDecimal("0.4"))).isInstanceOf(LoanDecisionRuleException.class);
    }
}
