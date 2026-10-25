
package com.grupob.bancobanquito.origination.internal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LoanApprovalPolicyTest {

    private final LoanApprovalPolicy policy =
            new LoanApprovalPolicy();

    @Test
    void shouldAllowRegisteredApplication() {
        assertDoesNotThrow(() ->
                policy.ensureRegistered("REGISTRADA"));
    }

    @Test
    void shouldRejectAlreadyApprovedApplication() {
        assertThrows(
                LoanDecisionRuleException.class,
                () -> policy.ensureRegistered("APROBADA")
        );
    }

    @Test
    void shouldRejectAlreadyRejectedApplication() {
        assertThrows(
                LoanDecisionRuleException.class,
                () -> policy.ensureRegistered("RECHAZADA")
        );
    }

    @Test
    void shouldAllowAnnualRateAtMaximum() {
        assertDoesNotThrow(() ->
                policy.validateAnnualEffectiveRate(
                        new BigDecimal("0.1677"),
                        new BigDecimal("0.1677")
                )
        );
    }

    @Test
    void shouldRejectRateAboveMaximum() {
        assertThrows(
                LoanDecisionRuleException.class,
                () -> policy.validateAnnualEffectiveRate(
                        new BigDecimal("0.1800"),
                        new BigDecimal("0.1677")
                )
        );
    }

    @Test
    void shouldAllowFrenchInstallment() {
        BigDecimal maximumInstallment =
                policy.validatePaymentCapacity(
                        new BigDecimal("3500.00"),
                        new BigDecimal("1200.00"),
                        new BigDecimal("911.44"),
                        new BigDecimal("0.4000")
                );

        assertEquals(
                new BigDecimal("920.00"),
                maximumInstallment
        );
    }

    @Test
    void shouldRejectGermanInstallment() {
        assertThrows(
                LoanDecisionRuleException.class,
                () -> policy.validatePaymentCapacity(
                        new BigDecimal("3500.00"),
                        new BigDecimal("1200.00"),
                        new BigDecimal("969.36"),
                        new BigDecimal("0.4000")
                )
        );
    }

    @Test
    void shouldRejectWhenRemainingIncomeIsZero() {
        assertThrows(
                LoanDecisionRuleException.class,
                () -> policy.validatePaymentCapacity(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("0.4000")
                )
        );
    }
}
